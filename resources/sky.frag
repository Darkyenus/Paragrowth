//Has prefix

varying vec3 direction;

void main() {
  float h = normalize(direction).z;
  gl_FragColor = vec4(0.0, h, 1.0, 1.0);
}