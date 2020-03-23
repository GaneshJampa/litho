/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import android.util.LongSparseArray;
import com.facebook.rendercore.RenderTreeNode;
import java.util.ArrayList;
import java.util.List;

/**
 * Can be passed to a MountState to override default mounting behaviour and control which items get
 * mounted or unmounted.
 */
public class MountDelegate {

  private final LongSparseArray<Integer> mReferenceCountMap = new LongSparseArray<>();
  private final List<MountDelegateExtension> mMountDelegateExtensions = new ArrayList<>();
  private final MountDelegateTarget mMountDelegateTarget;
  private boolean mReferenceCountingEnabled = false;

  // RenderCore MountState API
  interface MountDelegateTarget {
    void notifyMount(MountDelegateInput input, LayoutOutput layoutOutput, int position);

    void notifyUnmount(int position);

    boolean isRootItem(int position);

    // TODO: remove when ref counting for animations.
    boolean isAnimationLocked(int position);

    Object getContentAt(int position);
  }

  // IGNORE - Will be removed. Check out D4182567 for context.
  interface MountDelegateInput {
    int getLayoutOutputPositionForId(long id);

    RenderTreeNode getMountableOutputAt(int position);
  }

  public MountDelegate(MountDelegateTarget mountDelegateTarget) {
    mMountDelegateTarget = mountDelegateTarget;
  }

  void addExtension(MountDelegateExtension mountDelegateExtension) {
    mMountDelegateExtensions.add(mountDelegateExtension);
    mountDelegateExtension.registerToDelegate(this);
    mReferenceCountingEnabled =
        mReferenceCountingEnabled || mountDelegateExtension.canPreventMount();
  }

  // TODO remove this
  boolean isAnimationLocked(int position) {
    return mMountDelegateTarget.isAnimationLocked(position);
  }

  Object getContentAt(int position) {
    return mMountDelegateTarget.getContentAt(position);
  }

  boolean isRootItem(int position) {
    return mMountDelegateTarget.isRootItem(position);
  }

  boolean isLockedForMount(LayoutOutput layoutOutput) {
    if (!mReferenceCountingEnabled) {
      return true;
    }

    final long layoutOutputId = layoutOutput.getId();
    final Integer refCount = mReferenceCountMap.get(layoutOutputId);

    return refCount != null && refCount > 0;
  }

  void acquireMountRef(
      LayoutOutput layoutOutput, int i, MountDelegateInput input, boolean isMounting) {
    final boolean wasLockedForMount = isLockedForMount(layoutOutput);

    incrementExtensionRefCount(layoutOutput);

    // Only mount if we're during a mounting phase, otherwise the mounting phase will take care of
    // that.
    if (!wasLockedForMount && isMounting) {
      mMountDelegateTarget.notifyMount(input, layoutOutput, i);
    }
  }

  void releaseMountRef(LayoutOutput layoutOutput, int i, boolean isMounting) {
    final boolean wasLockedForMount = isLockedForMount(layoutOutput);
    decrementExtensionRefCount(layoutOutput);

    if (wasLockedForMount && !isLockedForMount(layoutOutput) && isMounting) {
      mMountDelegateTarget.notifyUnmount(i);
    }
  }

  void resetExtensionReferenceCount() {
    if (!mReferenceCountingEnabled) {
      return;
    }

    mReferenceCountMap.clear();
  }

  private void incrementExtensionRefCount(LayoutOutput layoutOutput) {
    if (!mReferenceCountingEnabled) {
      return;
    }

    final long layoutOutputId = layoutOutput.getId();
    Integer refCount = mReferenceCountMap.get(layoutOutputId);

    if (refCount == null) {
      refCount = 0;
    }

    mReferenceCountMap.put(layoutOutputId, refCount + 1);
  }

  private void decrementExtensionRefCount(LayoutOutput layoutOutput) {
    if (!mReferenceCountingEnabled) {
      return;
    }

    final long layoutOutputId = layoutOutput.getId();
    Integer refCount = mReferenceCountMap.get(layoutOutputId);

    if (refCount == null || refCount == 0) {
      throw new IllegalStateException(
          "Trying to decrement reference count for an item you don't own.");
    }

    mReferenceCountMap.put(layoutOutputId, refCount - 1);
  }
}
