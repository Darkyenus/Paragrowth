//Has prefix

varying vec3 direction;

const vec3 up = vec3(0.0,0.0,1.0);

void main() {
	vec3 dir = normalize(direction);
	float uppness = dot(dir,up);

	gl_FragColor = vec4(0.0, 1.0 - uppness, 1.0, 1.0);
}