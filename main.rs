extern crate gl;
extern crate winit;

use std::ffi::CString;
use std::mem;
use std::ptr;
use winit::event::{Event, WindowEvent};
use winit::event_loop::{ControlFlow, EventLoop};
use winit::window::WindowBuilder;

#[rustfmt::skip]
const VERTICES: [f32; 18] = [
    -0.5, -0.5, 0.0,    1.0, 0.0, 0.0,
     0.5, -0.5, 0.0,    0.0, 1.0, 0.0,
     0.0,  0.5, 0.0,    0.0, 0.0, 1.0,
];

const VS_SRC: &str = "
#version 330 core
layout (location = 0) in vec3 aPos;
void main() {
    gl_Position = vec4(aPos, 1.0);
}
";

const FS_SRC: &str = "
#version 330 core
out vec4 FragColor;
in vec3 ourColor;
void main() {
    FragColor = vec4(ourColor, 1.0);
}
";

fn compile_shader(src: &str, shader_type: gl::types::GLenum) -> u32 {
    let shader = unsafe { gl::CreateShader(shader_type) };
    let c_str = CString::new(src.as_bytes()).unwrap();
    unsafe {
        gl::ShaderSource(shader, 1, &c_str.as_ptr(), ptr::null());
        gl::CompileShader(shader);
    }
    shader
}

fn create_program(vs_src: &str, fs_src: &str) -> u32 {
    let vertex_shader = compile_shader(VS_SRC, gl::VERTEX_SHADER);
    let fragment_shader = compile_shader(FS_SRC, gl::FRAGMENT_SHADER);

    let program = unsafe { gl::CreateProgram() };
    unsafe {
        gl::AttachShader(program, vertex_shader);
        gl::AttachShader(program, fragment_shader);
        gl::LinkProgram(program);
        gl::DeleteShader(vertex_shader);
        gl::DeleteShader(fragment_shader);
    }
    program
}

fn main() {
    let event_loop = EventLoop::new();
    let window = WindowBuilder::new()
        .with_title("Redware: Triangle")
        .build(&event_loop)
        .unwrap();

    let gl_context = unsafe { glutin::ContextBuilder::new().build_windowed(window, &event_loop).unwrap() };
    let gl_context = unsafe { gl_context.make_current().unwrap() };

    gl::load_with(|symbol| gl_context.get_proc_address(symbol) as *const _);

    let program = create_program(VS_SRC, FS_SRC);

    let mut vbo: u32 = 0;
    unsafe {
        gl::GenBuffers(1, &mut vbo);
        gl::BindBuffer(gl::ARRAY_BUFFER, vbo);
        gl::BufferData(
            gl::ARRAY_BUFFER,
            (VERTICES.len() * mem::size_of::<f32>()) as isize,
            VERTICES.as_ptr() as *const _,
            gl::STATIC_DRAW,
        );
    }

    event_loop.run(move |event, _, control_flow| {
        *control_flow = ControlFlow::Wait;

        match event {
            Event::WindowEvent { event, .. } => match event {
                WindowEvent::CloseRequested => *control_flow = ControlFlow::Exit,
                _ => (),
            },
            Event::RedrawRequested(_) => {
                unsafe {
                    gl::ClearColor(0.2, 0.3, 0.3, 1.0);
                    gl::Clear(gl::COLOR_BUFFER_BIT);

                    gl::UseProgram(program);
                    gl::BindBuffer(gl::ARRAY_BUFFER, vbo);
                    gl::EnableVertexAttribArray(0);
                    gl::VertexAttribPointer(0, 3, gl::FLOAT, gl::FALSE, 6 * mem::size_of::<f32>() as i32, ptr::null());
                    gl::DrawArrays(gl::TRIANGLES, 0, 3);
                }
                gl_context.swap_buffers().unwrap();
            }
            _ => (),
        }
    });
}