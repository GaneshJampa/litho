/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.infer.annotation.ThreadSafe;
import com.facebook.litho.annotations.TreeProp;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A data structure to store tree props.
 *
 * @see TreeProp
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
@ThreadConfined(ThreadConfined.ANY)
public class TreeProps {

  private final Map<Class, Object> mMap = Collections.synchronizedMap(new HashMap<Class, Object>());

  public void put(Class key, @Nullable Object value) {
    mMap.put(key, value);
  }

  @Nullable
  public <T> T get(Class<T> key) {
    return (T) mMap.get(key);
  }

  /** @return a copy of the provided TreeProps instance; returns null if source is null */
  @ThreadSafe(enableChecks = false)
  public static @Nullable TreeProps copy(@Nullable TreeProps source) {
    if (source == null) {
      return null;
    }

    return acquire(source);
  }

  /**
   * Whenever a Spec sets tree props, the TreeProps map from the parent is copied. If parent
   * TreeProps are null, a new TreeProps instance is created to copy the current tree props.
   *
   * <p>Infer knows that newProps is owned but doesn't know that newProps.mMap is owned.
   */
  @ThreadSafe(enableChecks = false)
  public static TreeProps acquire(@Nullable TreeProps source) {
    final TreeProps newProps = new TreeProps();
    if (source != null) {
      synchronized (source.mMap) {
        newProps.mMap.putAll(source.mMap);
      }
    }

    return newProps;
  }

  void reset() {
    mMap.clear();
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TreeProps treeProps = (TreeProps) o;

    return mMap.equals(treeProps.mMap);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mMap);
  }
}
