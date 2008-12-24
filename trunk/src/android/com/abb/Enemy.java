// Copyright 2008 and onwards Matthew Burkhart.
//
// This program is free software; you can redistribute it and/or modify it under
// the terms of the GNU General Public License as published by the Free Software
// Foundation; version 3 of the License.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
// FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
// details.

package android.com.abb;

import android.graphics.Rect;
import android.view.KeyEvent;
import java.lang.Math;


public class Enemy extends Entity {
  public Enemy(Entity target) {
    super();
    mTarget = target;
    radius = kRadius;
    sprite_rect =
        new Rect(0, kSpriteBase, kSpriteWidth, kSpriteBase + kSpriteHeight);
  }

  public void step(float time_step) {
    super.step(time_step);
    ddy = kGravity;

    // If we have moved close enough to our target, mark it dead.
    if (Math.abs(mTarget.x - x) < kRadius &&
        Math.abs(mTarget.y - y) < kRadius) {
      mTarget.alive = false;
    }

    // Always move the enemy towards the target. Set the acceleration and sprite
    // to reflect it.
    int sprite_offset;
    if (mTarget.x < x) {
      sprite_offset = 0;
      ddx = -kAcceleration;
    } else {
      sprite_offset = 2;
      ddx = kAcceleration;
    }
    if (has_ground_contact) {
      ++sprite_offset;
      dy = -kJumpVelocity;
    }

    sprite_rect.top = kSpriteBase + kSpriteHeight * sprite_offset;
    sprite_rect.bottom = kSpriteBase + kSpriteHeight * (sprite_offset + 1);
  }

  private Entity mTarget;

  private static final float kAcceleration = 40.0f;
  private static final float kGravity = 100.0f;
  private static final float kJumpVelocity = 100.0f;
  private static final float kRadius = 20.0f;
  private static final int kSpriteBase = 0;
  private static final int kSpriteWidth = 64;
  private static final int kSpriteHeight = 64;
}