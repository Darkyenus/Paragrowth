//Has prefix, probably

attribute vec3 a_position;
uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;

varying vec3 direction;

void main() {
	vec4 up = vec4(0.0,0.0,1.0,1.0);
	vec4 dir = u_projViewTrans * vec4(a_position, 1.0);
	direction = dir.xyz;

	gl_Position = vec4(a_position, 1.0);
}