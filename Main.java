///////////////////////////////////////////////////////////
/// REDWARE engine in Java and implemented OBJ viewer    //
/// EXTREMELY UNFINISHED                                 //
///////////////////////////////////////////////////////////
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.*;
import java.nio.*;
import java.util.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Main {
    private long window;
    private int shaderProgram;
    private int vaoId;
    private int vboId;
    private int projMatrixLocation, modelMatrixLocation, viewMatrixLocation;

    public void run() {
        init();
        loop();

        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            throw new IllegalStateException("redware: unable to initialize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(800, 600, "redware: TEST", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("redware: failed to create GLFW window");
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);

            glfwGetWindowSize(window, pWidth, pHeight);

            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
        GL.createCapabilities();

        glEnable(GL_DEPTH_TEST);
        setupShaders();
        setupTriangle();
    }

    private void setupShaders() {
        String vertexShaderSource = "#version 330 core\n" +
                "layout(location = 0) in vec3 aPos;\n" +
                "uniform mat4 model;\n" +
                "uniform mat4 view;\n" +
                "uniform mat4 projection;\n" +
                "void main()\n" +
                "{\n" +
                "   gl_Position = projection * view * model * vec4(aPos, 1.0);\n" +
                "}\n";

        String fragmentShaderSource = "#version 330 core\n" +
                "out vec4 FragColor;\n" +
                "void main()\n" +
                "{\n" +
                "   FragColor = vec4(1.0, 0.5, 0.2, 1.0);\n" +
                "}\n";

        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, vertexShaderSource);
        glCompileShader(vertexShader);

        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, fragmentShaderSource);
        glCompileShader(fragmentShader);

        projMatrixLocation = glGetUniformLocation(shaderProgram, "projection");
        modelMatrixLocation = glGetUniformLocation(shaderProgram, "model");
        viewMatrixLocation = glGetUniformLocation(shaderProgram, "view");
    }

    private void setupTriangle() {
        float[] vertices = {
                -0.5f, -0.5f, 0.0f,
                 0.5f, -0.5f, 0.0f,
                 0.0f,  0.5f, 0.0f
        };

        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            glUseProgram(shaderProgram);

            Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(45.0f), 800f / 600f, 0.1f, 100.0f);
            Matrix4f view = new Matrix4f().lookAt(new Vector3f(0.0f, 0.0f, 3.0f), new Vector3f(0.0f, 0.0f, 0.0f), new Vector3f(0.0f, 1.0f, 0.0f));
            Matrix4f model = new Matrix4f().identity();

            FloatBuffer projBuffer = BufferUtils.createFloatBuffer(16);
            projection.get(projBuffer);
            glUniformMatrix4fv(projMatrixLocation, false, projBuffer);

            FloatBuffer viewBuffer = BufferUtils.createFloatBuffer(16);
            view.get(viewBuffer);
            glUniformMatrix4fv(viewMatrixLocation, false, viewBuffer);

            FloatBuffer modelBuffer = BufferUtils.createFloatBuffer(16);
            model.get(modelBuffer);
            glUniformMatrix4fv(modelMatrixLocation, false, modelBuffer);

            glBindVertexArray(vaoId);
            glDrawArrays(GL_TRIANGLES, 0, 3);
            glBindVertexArray(0);

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    public ModelData loadmodel(String filepath) {
        List<Vector3f> vertices = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        List<Vector2f> texCoords = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split("\\s+");
                switch (tokens[0]) {
                    case "v":
                        vertices.add(new Vector3f(Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]), Float.parseFloat(tokens[3])));
                        break;
                    case "vn":
                        normals.add(new Vector3f(Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]), Float.parseFloat(tokens[3])));
                        break;
                    case "vt":
                        texCoords.add(new Vector2f(Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2])));
                        break;
                    case "f":
                        for (int i = 1; i < tokens.length; i++) {
                            String[] parts = tokens[i].split("/");
                            indices.add(Integer.parseInt(parts[0]) - 1);
                        }
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return data;
    }

    public static class ModelData {
        public float[] vertices;
        public int vertexCount;
    }

    public static void main(String[] args) {
        new RedwareMain().run();
    }
}