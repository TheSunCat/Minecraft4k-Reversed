package minecraft4k;

import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class Minecraft4k
    extends JPanel
{
    static int[] input = new int[32767];
    
    final static int MOUSE_RIGHT = 0;
    final static int MOUSE_LEFT = 1;
    final static int MOUSE_X = 2;
    final static int MOUSE_Y = 3;
    
    final static int SCR_WIDTH = 214;
    final static int SCR_HEIGHT = 120;
    
    final static int WORLD_SIZE = 64;
    final static int WORLD_HEIGHT = 64;
    
    final static int BLOCK_AIR = 0;
    final static int BLOCK_GRASS = 1;
    final static int BLOCK_STONE = 4;
    final static int BLOCK_BRICKS = 5;
    final static int BLOCK_WOOD = 7;
    final static int BLOCK_LEAVES = 8;
    
    public static void main(String[] args)
    {
        JFrame frame = new JFrame("Minecraft4k");
        Minecraft4k m4k = new Minecraft4k();
        
        frame.addMouseListener(new MinecraftEventListener());
        frame.addMouseMotionListener(new MinecraftEventListener());
        frame.addKeyListener(new MinecraftEventListener());
        frame.getContentPane().add(m4k);
        frame.setSize(600, 400);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        
        m4k.run();
    }

    BufferedImage screen = new BufferedImage(SCR_WIDTH, SCR_HEIGHT, 1);
    
    public void run() {
        try {
            Random rand = new Random(18295169L);
            int[] screenBuffer = ((DataBufferInt)screen.getRaster().getDataBuffer()).getData();
            
            int[] world = new int[WORLD_SIZE * WORLD_HEIGHT * WORLD_SIZE];
            
            // fill world with random blocks
            for (int i = 0; i < world.length; i++) {
                int block;
                if(i / 64 % 64 > 32 + rand.nextInt(8))
                    block = (rand.nextInt(8) + 1);
                else
                    block = 0;
                
                world[i] = block;
            }
            
            int[] textureAtlas = new int[12288];
            // procedually generates the 16x3 textureAtlas with a tileSize of 16
            // gsd = grayscale detail
            for (int blockType = 1; blockType < 16; blockType++) {
                int gsd_tempA = 255 - rand.nextInt(96);

                for (int y = 0; y < 48; y++) {
                    for (int x = 0; x < 16; x++) {
                        // gets executed per pixel/texel

                        int tint = 0x966C4A; // brown (dirt)
                        
                        if (blockType != BLOCK_STONE || rand.nextInt(3) == 0) // if the block type is stone, update the noise value less often to get a streched out look
                            gsd_tempA = 255 - rand.nextInt(96);
                        
                        switch(blockType)
                        {
                            case BLOCK_STONE:
                                tint = 0x7F7F7F; // grey
                                break;
                            case BLOCK_GRASS:
                                if (y < (x * x * 3 + x * 81 >> 2 & 0x3) + 18) // grass + grass edge
                                    tint = 0x6AAA40; // green
                                else if (y < (x * x * 3 + x * 81 >> 2 & 0x3) + 19) // grass edge shadow
                                    gsd_tempA = gsd_tempA * 2 / 3;
                                break;
                            case BLOCK_WOOD:
                                tint = 0x675231; // brown (bark)
                                if (x > 0 && x < 15 && ((y > 0 && y < 15) || (y > 32 && y < 47))) { // wood inside area
                                    tint = 0xBC9862; // light brown

                                    // the following code repurposes 2 gsd variables making it a bit hard to read
                                    // but in short it gets the absulte distance from the tile's center in x and y direction 
                                    // finds the max of it
                                    // uses that to make the gray scale detail darker if the current pixel is part of an annual ring
                                    // and adds some noice as a finishig touch
                                    int gsd_final = x - 7;
                                    int gsd_tempB = (y & 0xF) - 7;

                                    if (gsd_final < 0)
                                        gsd_final = 1 - gsd_final;

                                    if (gsd_tempB < 0)
                                        gsd_tempB = 1 - gsd_tempB;

                                    if (gsd_tempB > gsd_final)
                                        gsd_final = gsd_tempB;

                                    gsd_tempA = 196 - rand.nextInt(32) + gsd_final % 3 * 32;
                                } else if (rand.nextInt(2) == 0) {
                                    // make the gsd 50% brighter on random pixels of the bark
                                    // and 50% darker if x happens to be odd
                                    gsd_tempA = gsd_tempA * (150 - (x & 1) * 100) / 100;
                                }
                                break;
                            case BLOCK_BRICKS:
                                tint = 0xB53A15; // red
                                if ((x + y / 4 * 4) % 8 == 0 || y % 4 == 0) // gap between bricks
                                    tint = 0xBCAFA5; // reddish light grey
                                break;
                                
                        }

                        int gsd_final = gsd_tempA;
                        if (y >= 32) // bottom side of the block
                            gsd_final /= 2; // has to be darker

                        if (blockType == BLOCK_LEAVES) {
                            tint = 0x50D937; // green
                            if (rand.nextInt(2) == 0) {
                                tint = 0;
                                gsd_final = 255;
                            }
                        }
                        // multiply tint by the grayscale detail
                        int col = (tint >> 16 & 0xFF) * gsd_final / 255 << 16 |
                                  (tint >>  8 & 0xFF) * gsd_final / 255 << 8 | 
                                  (tint       & 0xFF) * gsd_final / 255;

                        // write pixel to the texture atlas
                        textureAtlas[x + y * 16 + blockType * 256 * 3] = col;
                    }
                }
            }
            
            long startTime = System.currentTimeMillis();
            
            float playerX = 96.5F;
            float playerY = 65.0F; // higher val means lower
            float playerZ = 96.5F;
            float velocityX = 0.0F;
            float velocityY = 0.0F;
            float velocityZ = 0.0F;
            int hoveredBlock = -1; // index in world array
            int i5 = 0; // idk, changes depending on hovered block sort of
            float cameraYaw = 0.0F;
            float cameraPitch = 0.0F;
            
            while (true) {
                if(input[KeyEvent.VK_Q] == 1)
                {
                    System.out.println("DEBUG::BREAK");
                }
                
                float sinyaw = (float)Math.sin(cameraYaw);
                float cosYaw = (float)Math.cos(cameraYaw);
                float sinPitch = (float)Math.sin(cameraPitch);
                float cosPitch = (float)Math.cos(cameraPitch);
                
                while (System.currentTimeMillis() - startTime > 10L) {
                    if (input[MOUSE_X] > 0) {
                        float f13 = (input[MOUSE_X] - 428) / (float) SCR_WIDTH * 2.0F;
                        float f14 = (input[MOUSE_Y] - 240) / (float) SCR_HEIGHT * 2.0F;
                        float f15 = (float)Math.sqrt((f13 * f13 + f14 * f14)) - 1.2F;
                        
                        if (f15 < 0.0F)
                            f15 = 0.0F;
                        
                        if (f15 > 0.0F) {
                            cameraYaw += f13 * f15 / 400.0F;
                            cameraPitch -= f14 * f15 / 400.0F;
                            
                            if (cameraPitch < -1.57F)
                                cameraPitch = -1.57F;
                            
                            if (cameraPitch > 1.57F)
                                cameraPitch = 1.57F;
                        }
                    }
                    
                    startTime += 10L;
                    float inputX = (input[KeyEvent.VK_D] - input[KeyEvent.VK_A]) * 0.02F;
                    float inputZ = (input[KeyEvent.VK_W] - input[KeyEvent.VK_S]) * 0.02F;
                    velocityX *= 0.5F;
                    velocityY *= 0.99F;
                    velocityZ *= 0.5F;
                    velocityX += sinyaw * inputZ + cosYaw * inputX;
                    velocityZ += cosYaw * inputZ - sinyaw * inputX;
                    velocityY += 0.003F;
                    
                    
                    //check for movement on each axis individually (thanks JuPaHe64!)
                    OUTER:
                    for (int axisIndex = 0; axisIndex < 3; axisIndex++) {
                        float newPlayerX = playerX + velocityX * ((axisIndex + 0) % 3 / 2);
                        float newPlayerY = playerY + velocityY * ((axisIndex + 1) % 3 / 2);
                        float newPlayerZ = playerZ + velocityZ * ((axisIndex + 2) % 3 / 2);
                        
                        for (int colliderIndex = 0; colliderIndex < 12; colliderIndex++) {
                            // magic
                            int colliderBlockX = (int)(newPlayerX + (colliderIndex >> 0 & 1) * 0.6F - 0.3F) - WORLD_SIZE;
                            int colliderBlockY = (int)(newPlayerY + ((colliderIndex >> 2) - 1) * 0.8F + 0.65F) - WORLD_HEIGHT;
                            int colliderBlockZ = (int)(newPlayerZ + (colliderIndex >> 1 & 1) * 0.6F - 0.3F) - WORLD_SIZE;
                            
                            // check collision with world bounds and world blocks
                            if (colliderBlockX < 0 || colliderBlockY < 0 || colliderBlockZ < 0
                                    || colliderBlockX >= WORLD_SIZE || colliderBlockY >= WORLD_HEIGHT || colliderBlockZ >= WORLD_SIZE
                                    || world[colliderBlockX + colliderBlockY * WORLD_HEIGHT + colliderBlockZ * 4096] > BLOCK_AIR) {
                                
                                if (axisIndex != 1) //not checking for vertical movement
                                    break OUTER; //movement is invalid
                                
                                // if we're falling, colliding, and we press space
                                if (input[KeyEvent.VK_SPACE] > 0 && velocityY > 0.0F) {
                                    input[KeyEvent.VK_SPACE] = 0;
                                    velocityY = -0.1F; // jump
                                    break OUTER;
                                }
                                
                                // stop vertical movement (is this needed?)
                                velocityY = 0.0F;
                                break OUTER;
                            }
                        }
                        
                        playerX = newPlayerX;
                        playerY = newPlayerY;
                        playerZ = newPlayerZ;
                    }
                }
                
                System.out.println(playerY);
                
                int i6 = 0;
                int i7 = 0;
                
                // break block
                if (input[MOUSE_LEFT] == 1 && hoveredBlock > BLOCK_AIR) {
                    world[hoveredBlock] = BLOCK_AIR;
                    input[MOUSE_LEFT] = 0;
                }
                
                // place block
                if (input[MOUSE_RIGHT] > 0 && hoveredBlock > BLOCK_AIR) {
                    world[hoveredBlock + i5] = BLOCK_GRASS;
                    input[MOUSE_RIGHT] = 0;
                }
                
                for (int i8 = 0; i8 < 12; i8++) {
                    int magicX = (int)(playerX + (i8 >> 0 & 1) * 0.6F - 0.3F) - WORLD_SIZE;
                    int magicY = (int)(playerY + ((i8 >> 2) - 1) * 0.8F + 0.65F) - WORLD_HEIGHT;
                    int magicZ = (int)(playerZ + (i8 >> 1 & 1) * 0.6F - 0.3F) - WORLD_SIZE;
                    
                    // check if hovered block is within world boundaries
                    if (magicX >= 0 && magicY >= 0 && magicZ >= 0 && magicX < WORLD_SIZE && magicY < WORLD_HEIGHT && magicZ < WORLD_SIZE)
                        world[magicX + magicY * WORLD_HEIGHT + magicZ * 4096] = BLOCK_AIR;
                }
                
                // render the screen 214x120
                float newHoveredBlock = -1.0F;
                for (int x = 0; x < SCR_WIDTH; x++) {
                    float f18 = (x - 107) / 90.0F;
                    
                    for (int y = 0; y < SCR_HEIGHT; y++) {
                        float f20 = (y - 60) / 90.0F;
                        float f21 = 1.0F;
                        float f22 = f21 * cosPitch + f20 * sinPitch;
                        float f23 = f20 * cosPitch - f21 * sinPitch;
                        float f24 = f18 * cosYaw + f22 * sinyaw;
                        float f25 = f22 * cosYaw - f18 * sinyaw;
                        int i16 = 0;
                        int i17 = 255;
                        double d = 20.0D;
                        float f26 = 5.0F;
                        
                        for (int i18 = 0; i18 < 3; i18++) {
                            float f27 = f24;
                            if (i18 == 1)
                                f27 = f23;
                            
                            if (i18 == 2)
                                f27 = f25;
                            
                            float f28 = 1.0F / ((f27 < 0.0F) ? -f27 : f27);
                            float f29 = f24 * f28;
                            float f30 = f23 * f28;
                            float f31 = f25 * f28;
                            float f32 = playerX - (int)playerX;
                            
                            if (i18 == 1)
                                f32 = playerY - (int)playerY;
                            
                            if (i18 == 2)
                                f32 = playerZ - (int)playerZ;
                            
                            if (f27 > 0.0F)
                                f32 = 1.0F - f32;
                            
                            float f33 = f28 * f32;
                            float f34 = playerX + f29 * f32;
                            float f35 = playerY + f30 * f32;
                            float f36 = playerZ + f31 * f32;
                            
                            if (f27 < 0.0F) {
                                if (i18 == 0)
                                    f34--;
                                
                                if (i18 == 1)
                                    f35--;
                                
                                if (i18 == 2)
                                    f36--;
                            }
                            
                            while (f33 < d) {
                                int i21 = (int)f34 - 64;
                                int i22 = (int)f35 - 64;
                                int i23 = (int)f36 - 64;
                                
                                if (i21 < 0 || i22 < 0 || i23 < 0 || i21 >= 64 || i22 >= 64 || i23 >= 64)
                                    break;
                                
                                int i24 = i21 + i22 * 64 + i23 * 4096;
                                int i25 = world[i24];
                                
                                if (i25 > 0) {
                                    i6 = (int)((f34 + f36) * 16.0F) & 0xF;
                                    i7 = ((int)(f35 * 16.0F) & 0xF) + 16;
                                    
                                    if (i18 == 1) {
                                        i6 = (int)(f34 * 16.0F) & 0xF;
                                        i7 = (int)(f36 * 16.0F) & 0xF;
                                        
                                        if (f30 < 0.0F)
                                            i7 += 32;
                                    }
                                    
                                    int i26 = 16777215;
                                    if (i24 != hoveredBlock || (i6 > 0 && i7 % 16 > 0 && i6 < 15 && i7 % 16 < 15))
                                        i26 = textureAtlas[i6 + i7 * 16 + i25 * 256 * 3];
                                    
                                    if (f33 < f26 && x == input[MOUSE_X] / 4 && y == input[MOUSE_Y] / 4) {
                                        newHoveredBlock = i24;
                                        i5 = 1;
                                        if (f27 > 0.0F)
                                            i5 = -1;
                                        
                                        i5 <<= 6 * i18;
                                        f26 = f33;
                                    }
                                    
                                    if (i26 > 0) {
                                        i16 = i26;
                                        i17 = 255 - (int)(f33 / 20.0F * 255.0F);
                                        i17 = i17 * (255 - (i18 + 2) % 3 * 50) / 255;
                                        d = f33;
                                    }
                                }
                                
                                f34 += f29;
                                f35 += f30;
                                f36 += f31;
                                f33 += f28;
                            }
                        }
                        
                        int i18 = (i16 >> 16 & 0xFF) * i17 / 255;
                        int i19 = (i16 >> 8 & 0xFF) * i17 / 255;
                        int i20 = (i16 & 0xFF) * i17 / 255;
                        screenBuffer[x + y * SCR_WIDTH] = i18 << 16 | i19 << 8 | i20;
                    }
                }
                
                hoveredBlock = (int) newHoveredBlock;
                
                Thread.sleep(2L);
                repaint();
            }
        
        }catch (Exception localException) {
            localException.printStackTrace();
        }
    }
    
    @Override
    public void paint(Graphics g)
    {
        g.drawImage(screen, 0, 0, 856, 480, null);
    }
}

class MinecraftEventListener implements KeyListener, MouseListener, MouseMotionListener
{
    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        Minecraft4k.input[e.getKeyCode()] = 1;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        Minecraft4k.input[e.getKeyCode()] = 0;
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        Minecraft4k.input[Minecraft4k.MOUSE_X] = e.getX();
        Minecraft4k.input[Minecraft4k.MOUSE_Y] = e.getY();
        
        if (e.isMetaDown()) {
            Minecraft4k.input[Minecraft4k.MOUSE_LEFT] = 1;
            return;
        }
        Minecraft4k.input[Minecraft4k.MOUSE_RIGHT] = 1;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isMetaDown()) {
            Minecraft4k.input[Minecraft4k.MOUSE_LEFT] = 0;
            return;
        }
        Minecraft4k.input[Minecraft4k.MOUSE_RIGHT] = 0;
    }

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {
        Minecraft4k.input[Minecraft4k.MOUSE_X] = 0;
        Minecraft4k.input[Minecraft4k.MOUSE_Y] = 0;
    }

    @Override
    public void mouseDragged(MouseEvent e) {}

    @Override
    public void mouseMoved(MouseEvent e) {
        Minecraft4k.input[Minecraft4k.MOUSE_X] = e.getX();
        Minecraft4k.input[Minecraft4k.MOUSE_Y] = e.getY();
    }
}