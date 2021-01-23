Minecraft4k-Reversed was created by me and JuPaHe64.

Controls:
Move: WASD
Jump: Space
Break block: Left Click
Place block: Right Click
Cycle through inventory: Scroll
Higher resolution: Dot
Lower resolution: Comma


This project is a reverse-engineered version of Notch's submission for the 2010 Java 4k Contest, where one would submit Java programs of 4kb or under in size.

Notch submitted Minecraft4k, a heavily optimized and limited Minecraft port. However, the game contained a multitude of bugs and is difficult to play today.
The original build can be found at https://archive.org/details/Minecraft4K.
There is a Minecraft wiki page for Minecraft4k: https://minecraft.gamepedia.com/Minecraft_4k#:~:text=Minecraft%204k%20was%20an%20edition,(4096%20bytes)%20in%20size.

Minecraft4k-Reversed was created by decompiling this jarfile, and documenting what most of the code does. After fixing some bugs related to movement and collision detection, we moved to expand the game. This resulted in proper terrain generation, raycasted shadows, better graphics, and huge performance improvements thanks to multithreading raymarching on the CPU. However, it became difficult to manage and improve performance further, so we moved to developing a C++ port which uses the GPU for raymarching: https://github.com/RealTheSunCat/Minecraft4k-CPP

Some interesting things:
- Textures take too much space to store. They are generated on game start by algorithms!
- Notch wrote a custom voxel raymarching algorithm to avoid the filesize penalty of using a rasterizer library
- Block shading is "baked in", where the bottom of every block has extra grayscale applied to it
