#version 330

in vec3 direction;
out vec4 fragmentColor;

uniform vec3 u_cameraUp = vec3(0.0, 0.0, 1.0);

uniform vec4 u_low_color;
uniform vec4 u_high_color;

void main() {
	vec3 dir = normalize(direction);
	float uppness = dot(dir, u_cameraUp);

	fragmentColor = vec4(mix(u_low_color.rgb, u_high_color.rgb, uppness), 1.0);
}