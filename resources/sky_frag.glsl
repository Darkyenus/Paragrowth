#version 330

in vec3 direction;
out vec4 fragmentColor;

uniform vec3 u_cameraUp = vec3(0.0, 0.0, 1.0);

void main() {
	vec3 dir = normalize(direction);
	float uppness = dot(dir, u_cameraUp);

	fragmentColor = vec4(0.0, 1.0 - uppness, 1.0, 1.0);
}