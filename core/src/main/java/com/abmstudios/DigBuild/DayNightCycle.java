package com.abmstudios.DigBuild;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.MathUtils;

public class DayNightCycle {

    /*
     * Normal full-day length:
     * 20 real minutes.
     */
    private static final float DAY_LENGTH = 1200f;

    /*
     * Debug speed multiplier.
     *
     * Holding T makes the day/night cycle 30x faster.
     */
    private static final float DEBUG_SPEED = 30f;

    private float timeOfDay = 0.25f;

    private final DirectionalLight sun;

    private float sunlightIntensity;

    public DayNightCycle(Environment environment) {

        sun = new DirectionalLight();

        environment.add(sun);

        updateSun();
    }

    public void update(float delta) {

        /*
         * Check whether the debug key is being held.
         */
        boolean debugSpeed =
            com.badlogic.gdx.Gdx.input.isKeyPressed(
                com.badlogic.gdx.Input.Keys.T
            );

        /*
         * Apply the debug multiplier.
         */
        float speed = debugSpeed
            ? DEBUG_SPEED
            : 1f;

        /*
         * Advance time.
         */
        timeOfDay +=
            (delta / DAY_LENGTH) * speed;

        /*
         * Loop back around after midnight.
         */
        if (timeOfDay >= 1f) {
            timeOfDay -= 1f;
        }

        updateSun();
    }

    private void updateSun() {

        /*
         * Convert the time of day into an angle.
         */
        float angle =
            (timeOfDay - 0.25f) *
                MathUtils.PI2;

        /*
         * Height of the sun.
         */
        float sunHeight =
            MathUtils.sin(angle);

        /*
         * Horizontal movement.
         */
        float sunX =
            MathUtils.cos(angle);

        /*
         * Direction of the sunlight.
         */
        float x = -sunX;
        float y = -sunHeight;
        float z = 0.35f;

        /*
         * Calculate how much daylight there is.
         */
        float daylight = MathUtils.clamp(
            (sunHeight + 0.12f) / 0.30f,
            0f,
            1f
        );

        daylight =
            daylight *
                daylight *
                (3f - 2f * daylight);

        /*
         * Night colour.
         */
        float nightR = 0.12f;
        float nightG = 0.16f;
        float nightB = 0.30f;

        /*
         * Sunrise/sunset colour.
         */
        float sunsetR = 1.0f;
        float sunsetG = 0.48f;
        float sunsetB = 0.20f;

        /*
         * Day colour.
         */
        float dayR = 1.0f;
        float dayG = 0.95f;
        float dayB = 0.82f;

        /*
         * Blend night -> sunset.
         */
        float blendSunset =
            MathUtils.clamp(
                daylight * 2f,
                0f,
                1f
            );

        float r =
            MathUtils.lerp(
                nightR,
                sunsetR,
                blendSunset
            );

        float g =
            MathUtils.lerp(
                nightG,
                sunsetG,
                blendSunset
            );

        float b =
            MathUtils.lerp(
                nightB,
                sunsetB,
                blendSunset
            );

        /*
         * Blend sunset -> day.
         */
        float blendDay =
            MathUtils.clamp(
                (daylight - 0.5f) * 2f,
                0f,
                1f
            );

        r = MathUtils.lerp(
            r,
            dayR,
            blendDay
        );

        g = MathUtils.lerp(
            g,
            dayG,
            blendDay
        );

        b = MathUtils.lerp(
            b,
            dayB,
            blendDay
        );

        /*
         * Sunlight brightness.
         */
        sunlightIntensity =
            0.12f +
                daylight * 0.88f;

        /*
         * Apply the sun.
         */
        sun.set(
            r * sunlightIntensity,
            g * sunlightIntensity,
            b * sunlightIntensity,
            x,
            y,
            z
        );
    }

    public float getTimeOfDay() {
        return timeOfDay;
    }

    public float getDaylight() {

        float angle =
            (timeOfDay - 0.25f) *
                MathUtils.PI2;

        float sunHeight =
            MathUtils.sin(angle);

        float daylight = MathUtils.clamp(
            (sunHeight + 0.12f) / 0.30f,
            0f,
            1f
        );

        return daylight *
            daylight *
            (3f - 2f * daylight);
    }

    public void setTimeOfDay(float time) {

        timeOfDay = time % 1f;

        if (timeOfDay < 0f) {
            timeOfDay += 1f;
        }

        updateSun();
    }

    public float getHours() {
        return timeOfDay * 24f;
    }

    public DirectionalLight getSun() {
        return sun;
    }

    public void dispose() {
    }
}
