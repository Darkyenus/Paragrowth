#version 330

flat in vec4 v_color;
out vec4 fragmentColor;

// Dither code: Looks great, but very slow when intersecting
//#define DITHER
#ifdef DITHER
const float truncDepth = 3.0;
const float dScl = truncDepth/64.0;

// From https://devlog-martinsh.blogspot.com/2011/03/glsl-8x8-bayer-matrix-dithering.html
const float dither[64] = float[](
0*dScl, 32*dScl, 8*dScl, 40*dScl, 2*dScl, 34*dScl, 10*dScl, 42*dScl,
48*dScl, 16*dScl, 56*dScl, 24*dScl, 50*dScl, 18*dScl, 58*dScl, 26*dScl,
12*dScl, 44*dScl, 4*dScl, 36*dScl, 14*dScl, 46*dScl, 6*dScl, 38*dScl,
60*dScl, 28*dScl, 52*dScl, 20*dScl, 62*dScl, 30*dScl, 54*dScl, 22*dScl,
3*dScl, 35*dScl, 11*dScl, 43*dScl, 1*dScl, 33*dScl, 9*dScl, 41*dScl,
51*dScl, 19*dScl, 59*dScl, 27*dScl, 49*dScl, 17*dScl, 57*dScl, 25*dScl,
15*dScl, 47*dScl, 7*dScl, 39*dScl, 13*dScl, 45*dScl, 5*dScl, 37*dScl,
63*dScl, 31*dScl, 55*dScl, 23*dScl, 61*dScl, 29*dScl, 53*dScl, 21*dScl
);
#endif

void main() {
#ifdef DITHER
	float depth = gl_FragCoord.z / gl_FragCoord.w;
	const float scale = 1.0 / 4.0;
	int ditherX = int(mod(gl_FragCoord.x * scale, 8));
	int ditherY = int(mod(gl_FragCoord.y * scale, 8));


	float d = dither[ditherX + ditherY * 8];
	if (depth < d) {
		discard;
	}
#endif DITHER

	fragmentColor = v_color;
}
