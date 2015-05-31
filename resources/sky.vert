//Has prefix

attribute vec3 a_position;
uniform mat4 u_viewTurnMat; //u_projViewTrans without translation

varying vec3 direction;

void main() {
	direction = normalize(a_position);

	gl_Position = u_viewTurnMat * vec4(a_position, 1.0);
}
