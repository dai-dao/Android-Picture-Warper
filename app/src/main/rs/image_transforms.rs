#pragma version(1)
#pragma rs_fp_relaxed
#pragma rs java_package_name(com.example.pictureapp)
#include "rs_core.rsh"


#define MATH_PI 3.141592653589793238462643383279502884197169399375

// These are inputs to be passed in from Java code
const uchar4* input;
int width;
int height;

// Method to avoid edge case if calculated coordinates are
// out of bounds
static uchar4 clampPixel(int x, int y){
    if (y >= height) y = height-1;
    if (y < 0) y = 0;
    if (x >= width) x = width-1;
    if (x < 0) x = 0;
    // Because the image is represented as a flattened array
    return input[y*width + x];
}

// Reference: https://stackoverflow.com/questions/5055625/image-warping-bulge-effect-algorithm
uchar4 __attribute__((kernel)) bulge(uchar4 in, uint32_t x, uint32_t y)
{
    float x0, y0;
    float x_norm, y_norm;
    float r, a, rn;
    int srcX, srcY;

    x0 = 0.5f * (width - 1);
    y0 = 0.5f * (height - 1);

    // Here we want image coordinates to be between [0, 1]
    x_norm = (float) x / (float) width;
    y_norm = (float) y / (float) height;

    r = sqrt(pow(x_norm - 0.5f, 2) + pow(y_norm - 0.5f, 2));
    a = atan2(x_norm - 0.5f, y_norm - 0.5f);
    rn = pow(r, 2.5f) / 0.5f;

    srcX = rn * cos(a) + 0.5f;
    srcY = rn * sin(a) + 0.5f;

    // Normalize coordinates back to [0, 255]
    srcX = (int) (srcX * (float) width);
    srcY = (int) (srcY * (float) height);

    return clampPixel(srcX, srcY);
}

// Java Reference: https://introcs.cs.princeton.edu/java/31datatype/Swirl.java.html
uchar4 __attribute__((kernel)) swirl(uchar4 in, uint32_t x, uint32_t y)
{
	float x0, y0, dx, dy;
    float radius, angle;
    int srcX, srcY;

	x0 = 0.5f * (width - 1);
	y0 = 0.5f * (height - 1);

    dy = y0 - y;
    dx = x - x0;

    radius = sqrt(dx*dx + dy*dy);
    angle = MATH_PI / 256.0f * radius;

    srcX = (int) (dx * cos(angle) - dy * sin(angle));
    srcY = (int) (dx * sin(angle) + dy * cos(angle));

    srcX += x0;
	srcY += y0;
	srcY = height - srcY;

    return clampPixel(srcX, srcY);
}

// Reference: http://www.tannerhelland.com/4743/simple-algorithm-correcting-lens-distortion/
uchar4 __attribute__((kernel)) fisheye(uchar4 in, uint32_t x, uint32_t y)
{
    float x0, y0, dx, dy;
    float dist, r, theta, correction_radius;
    int srcX, srcY;

    float strength = 0.00001;
    float zoom = 1.1;

    correction_radius = sqrt((float) (width*width + height*height));
    correction_radius /= strength;
    x0 = 0.5f * (width - 1);
    y0 = 0.5f * (height - 1);

    dy = y0 - y;
    dx = x - x0;

    dist = sqrt(dx*dx + dy*dy);
    r = dist / correction_radius;

    if (r == 0.0f) {
        theta = 1.0f;
    } else {
        theta = atan(r) / r;
    }

    srcX = (int) (x0 + theta * dx * zoom);
    srcY = (int) (y0 + theta * dy * zoom);
    return clampPixel(srcX, srcY);
}



