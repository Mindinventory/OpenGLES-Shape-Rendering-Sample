uniform mat4 uVPMatrix;
attribute vec4 a_Position;

void main(void)
{
    gl_Position =  uVPMatrix * a_Position;
}

