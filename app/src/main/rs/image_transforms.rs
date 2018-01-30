#pragma version(1)
#pragma rs_fp_relaxed
#pragma rs java_package_name(com.example.pictureapp)

// Reference: https://github.com/Jtfinlay/PhotoWarp/blob/master/app/src/main/rs/transform.rs
#define C_PI 3.141592653589793238462643383279502884197169399375
#include "rs_core.rsh"

const uchar4* input;
int width;
int height;

static uchar4 getPixelAt(int, int);

uchar4 __attribute__((kernel)) bulge(uchar4 in, uint32_t x, uint32_t y)
{
	float r, a, rn, cX, cY;


	float xdist, ydist;
	float srcX, srcY;

	cX = (float) width / 2.0f;
   	cY = (float) height / 2.0f;

	xdist = ((float) x / (float)width);
	ydist = ((float) y / (float)height);

	r = sqrt(pow((xdist-0.5f), 2) + pow((ydist-0.5f), 2));
	a = atan2((xdist-0.5f), (ydist-0.5f));
	rn = pow(r,2.0f)/0.5f;

	srcX = rn * sin(a) + 0.5f;
	srcY = rn * cos(a) + 0.5f;
	srcX *= (float) width;
	srcY *= (float) height;

	return getPixelAt((int)srcX, (int)srcY);
}



uchar4 __attribute__((kernel)) swirl(uchar4 in, uint32_t x, uint32_t y)
{
	int srcX, srcY;
	float relX, relY, cX, cY;
	float angle, new_angle, radius;

	cX = (float) width / 2.0f;
	cY = (float) height / 2.0f;
	relY = cY-y;

	relX = x - cX;
	if (relX != 0)
	{
		angle = atan( fabs(relY) / fabs(relX));
		if (relX > 0 && relY < 0) angle = 2.0f * C_PI - angle;
		else if (relX <= 0 && relY >= 0) angle = C_PI - angle;
		else if (relX <=0 && relY < 0) angle += C_PI;
	}
	else
	{
		if (relY >= 0) angle = 0.5f * C_PI;
		else angle = 1.5f * C_PI;
	}

    radius = sqrt( relX*relX + relY*relY);
	new_angle = angle + (.001f * radius);

	srcX = (int)(radius * cos(new_angle)+0.5f);
	srcY = (int)(radius * sin(new_angle)+0.5f);
	srcX += cX;
	srcY += cY;
	srcY = height - srcY;

	return getPixelAt(srcX, srcY);
}


uchar4 __attribute__((kernel)) fisheye(uchar4 in, uint32_t x, uint32_t y)
{
	float ny, nx, r, nr;
	float theta, nxn, nyn;
	int x2, y2;
	ny = ((2.0f * y) / height) - 1.0f;
	nx = ((2.0f * x) / width) - 1.0f;
	r = sqrt(nx*nx + ny*ny);

	if (0.0f <= r && r <= 1.0)
	{
		nr = (r + (1.0f - nr)) / 2.0f;

    	if (nr <= 1.0)
		{
          	theta = atan2(ny, nx);
           	nxn = nr * cos(theta);
           	nyn = nr * sin(theta);

           	x2 = (int) (((nxn+1)*width) / 2.0f);
    		y2 = (int) (((nyn+1)*height) / 2.0f);

	     	return getPixelAt(x2, y2);
		}
	}
	return 0;
}



//a convenience method to clamp getting pixels into the image
static uchar4 getPixelAt(int x, int y) {
	if(y>=height) y = height-1;
	if(y<0) y = 0;
	if(x>=width) x = width-1;
	if(x<0) x = 0;
	return input[y*width + x];
}



