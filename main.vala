/////////////////////////////////////////////
///////REDWARE: TRIANGL Vala edition////////
///////////////////////////////////////////

using Gtk;
using Gdk;
using Epoxy;

class GLtriangleapp : Gtk.Application {
    private uint vao;
    private uint vbo;

    private const float[] vertices = {
        0.0f,  0.5f, 0.0f,    1.0f, 0.0f, 0.0f,
       -0.5f, -0.5f, 0.0f,    0.0f, 1.0f, 0.0f,
        0.5f, -0.5f, 0.0f,    0.0f, 0.0f, 1.0f
    };

    private const string vertexShaderSource = """
        #version 330 core
        layout (location = 0) in vec3 aPos;
        void main() {
            gl_Position = vec4(aPos, 1.0);
        }
    """;

    private const string fragmentShaderSource = """
        #version 330 core
        out vec4 FragColor;
        void main() {
            FragColor = vec4(1.0, 0.5, 0.2, 1.0);
        }
    """;

    public GLtriangleapp() {
        Object(application_id: "org.gtk.example.GLtriangleapp");
    }

    protected override void activate() {
        var window = new Gtk.ApplicationWindow(this) {
            title = "Redware engine GLArea example",
            default_width = 800,
            default_height = 600
        };

        var glArea = new Gtk.GLArea();
        glArea.realize.connect(on_gl_area_realize);
        glArea.render.connect(on_gl_area_render);
        glArea.unrealize.connect(on_gl_area_unrealize);

        window.add(glArea);
        window.show_all();
    }

    private void on_gl_area_realize (Gtk.GLArea area) {
        area.make_current();

        if (area.get_error() != null) {
            stderr.printf("redware: failed: %s\n", area.get_error().message);
            return;
        }

        initOpenGL();
    }

    private bool on_gl_area_render (Gtk.GLArea area, GLContext context) {
        glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);

        glUseProgram(shaderProgram);
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 3);

        return true;
    }

    private void on_gl_area_unrealize (Gtk.GLArea area) {
        glDeleteVertexArrays(1, &vao);
        glDeleteBuffers(1, &vbo);
        glDeleteProgram(shaderProgram);
    }

    private uint create_program () {
        uint vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, 1, &vertexShaderSource, null);
        glCompileShader(vertexShader);

        uint fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, 1, &fragmentShaderSource, null);
        glCompileShader(fragmentShader);

        uint program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        return program;
    }

    private uint compile_shader (uint type, string source) {
        uint shader = glCreateShader(type);
        glShaderSource(shader, 1, &source, null);
        glCompileShader(shader);

        int success;
        glGetShaderiv(shader, GL_COMPILE_STATUS, &success);
        if (success == GL_FALSE) {
            char[] infoLog = new char[512];
            glGetShaderInfoLog(shader, 512, null, infoLog);
            stderr.printf("ERROR::SHADER::COMPILATION_FAILED\n%s\n", infoLog);
        }

        return shader;
    }

    public static int main (string[] args) {
        var app = new GLtriangleapp();
        return app.run(args);
    }
}