#version 330

in vec3 a_position;
out vec3 direction;

//u_projViewTrans without translation
uniform mat4 u_viewTurnMat;

void main() {
	direction = normalize(a_position);

	gl_Position = u_viewTurnMat * vec4(a_position, 1.0);
}
