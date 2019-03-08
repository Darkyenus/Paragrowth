#version 330

#ifdef GL_ES
precision mediump float;
#endif

in vec4 v_col;
out vec4 fragmentColor;

void main() {
    fragmentColor = v_col;
}