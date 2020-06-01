package minecraft4k;

import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import static minecraft4k.Minecraft4k.*;

public class Minecraft4k
    extends JPanel
{
    static int[] input = new int[32767];
    
    //static java.awt.Point lastMousePosition = new java.awt.Point(0, 0);
    static java.awt.Point mouseDelta = new java.awt.Point(0, 0);
    
    final static int MOUSE_RIGHT = 0;
    final static int MOUSE_LEFT = 1;
    
    final static int SCR_WIDTH = 214;
    final static int SCR_HEIGHT = 120;
    
    final static int WINDOW_WIDTH = 856;
    final static int WINDOW_HEIGHT = 480;
    
    final static int WORLD_SIZE = 64;
    final static int WORLD_HEIGHT = 64;
    
    final static int AXIS_X = 0;
    final static int AXIS_Y = 1;
    final static int AXIS_Z = 2;
    
    final static int BLOCK_AIR = 0;
    final static int BLOCK_GRASS = 1;
    final static int BLOCK_STONE = 4;
    final static int BLOCK_BRICKS = 5;
    final static int BLOCK_WOOD = 7;
    final static int BLOCK_LEAVES = 8;
    
    final static int TEXTURE_SIZE = 16;
    
    public static void main(String[] args)
    {
        JFrame frame = new JFrame("Minecraft4k");
        Minecraft4k m4k = new Minecraft4k();
        
        frame.addMouseListener(new MinecraftEventListener());
        frame.addMouseMotionListener(new MinecraftEventListener());
        frame.addKeyListener(new MinecraftEventListener());
        
        frame.setSize(856, 480);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null); // center the window
        
        // hide the cursor
        frame.setCursor(frame.getToolkit().createCustomCursor(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), new java.awt.Point(), null));
        
        // add Minecraft!
        frame.getContentPane().add(m4k);
        
        frame.setVisible(true);
        m4k.run();
    }

    BufferedImage screen = new BufferedImage(SCR_WIDTH, SCR_HEIGHT, 1);
    
    public void run() {
        try {
            java.util.Random rand = new java.util.Random(18295169L);
            int[] screenBuffer = ((DataBufferInt) screen.getRaster().getDataBuffer()).getData();
            
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
            
            int[] textureAtlas = new int[16 * 3 * TEXTURE_SIZE * TEXTURE_SIZE];
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
            int placeBlockOffset = 0; // idk, changes depending on hovered block sort of
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
                    // adjust camera
                    cameraYaw += mouseDelta.x / 400.0F;
                    cameraPitch -= mouseDelta.y / 400.0F;

                    if (cameraPitch < -1.57F)
                        cameraPitch = -1.57F;

                    if (cameraPitch > 1.57F)
                        cameraPitch = 1.57F;
                    
                    
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
                        float newPlayerX = playerX + velocityX * ((axisIndex + AXIS_Y) % 3 / 2);
                        float newPlayerY = playerY + velocityY * ((axisIndex + AXIS_X) % 3 / 2);
                        float newPlayerZ = playerZ + velocityZ * ((axisIndex + AXIS_Z) % 3 / 2);
                        
                        for (int colliderIndex = 0; colliderIndex < 12; colliderIndex++) {
                            // magic
                            int colliderBlockX = (int)(newPlayerX + (colliderIndex >> 0 & 1) * 0.6F - 0.3F) - WORLD_SIZE;
                            int colliderBlockY = (int)(newPlayerY + ((colliderIndex >> 2) - 1) * 0.8F + 0.65F) - WORLD_HEIGHT;
                            int colliderBlockZ = (int)(newPlayerZ + (colliderIndex >> 1 & 1) * 0.6F - 0.3F) - WORLD_SIZE;
                            
                            // check collision with world bounds and world blocks
                            if (colliderBlockX < 0 || colliderBlockY < 0 || colliderBlockZ < 0
                                    || colliderBlockX >= WORLD_SIZE || colliderBlockY >= WORLD_HEIGHT || colliderBlockZ >= WORLD_SIZE
                                    || world[colliderBlockX + colliderBlockY * WORLD_HEIGHT + colliderBlockZ * 4096] > BLOCK_AIR) {
                                
                                if (axisIndex != AXIS_Z) //not checking for vertical movement
                                    continue OUTER; //movement is invalid
                                
                                // if we're falling, colliding, and we press space
                                if (input[KeyEvent.VK_SPACE] == 1 && velocityY > 0.0F) {
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
                
                int i6 = 0;
                int i7 = 0;
                
                // break block
                if (input[MOUSE_LEFT] == 1 && hoveredBlock > BLOCK_AIR) {
                    world[hoveredBlock] = BLOCK_AIR;
                    input[MOUSE_LEFT] = 0;
                }
                
                // place block
                if (input[MOUSE_RIGHT] > 0 && hoveredBlock > BLOCK_AIR) {
                    world[hoveredBlock + placeBlockOffset] = BLOCK_GRASS;
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
                
                // render the screen
                float newHoveredBlock = -1.0F;
                for (int screenX = 0; screenX < SCR_WIDTH; screenX++) {
                    float xDistSmall = (screenX - (SCR_WIDTH / 2)) / 90.0F;
                    
                    for (int screenY = 0; screenY < SCR_HEIGHT; screenY++) {
                        float yDistSmall = (screenY - (SCR_HEIGHT / 2)) / 90.0F;
                        
                        float temp = cosPitch + yDistSmall * sinPitch;
                        
                        float rayDirX = xDistSmall * cosYaw + temp * sinyaw;
                        float rayDirY = yDistSmall * cosPitch - sinPitch;
                        float rayDirZ = temp * cosYaw - xDistSmall * sinyaw;
                        
                        int i16 = 0;
                        int fogIntensity = 0x00;
                        double renderDistSorta = 20.0D;
                        float playerReach = 5.0F;
                        
                        for (int axis = 0; axis < 3; axis++) {
                            float delta;
                            switch(axis)
                            {
                                default:
                                case AXIS_X:
                                    delta = rayDirX;
                                    break;
                                case AXIS_Y:
                                    delta = rayDirY;
                                    break;
                                case AXIS_Z:
                                    delta = rayDirZ;
                                    break;
                            }
                            
                            float f28 = 1.0f / Math.abs(delta);
                            
                            
                            float rayDeltaX = rayDirX * f28;
                            float rayDeltaY = rayDirY * f28;
                            float rayDeltaZ = rayDirZ * f28;
                            
                            
                            float floatComponent;
                            switch(axis)
                            {
                                default:
                                case AXIS_X:
                                    floatComponent = playerX % 1.0f;
                                    break;
                                case AXIS_Y:
                                    floatComponent = playerY % 1.0f;
                                    break;
                                case AXIS_Z:
                                    floatComponent = playerZ % 1.0f;
                                    break;
                            }
                            
                            if (delta > 0)
                                floatComponent = 1.0f - floatComponent;
                            
                            float rayTravelDist = f28 * floatComponent;
                            
                            float rayX = playerX + rayDeltaX * floatComponent;
                            float rayY = playerY + rayDeltaY * floatComponent;
                            float rayZ = playerZ + rayDeltaZ * floatComponent;
                            
                            if (delta < 0.0F) {
                                if (axis == 0)
                                    rayX--;
                                
                                if (axis == 1)
                                    rayY--;
                                
                                if (axis == 2)
                                    rayZ--;
                            }
                            
                            while (rayTravelDist < renderDistSorta) {
                                int i21 = (int) rayX - 64;
                                int i22 = (int) rayY - 64;
                                int i23 = (int) rayZ - 64;
                                
                                if (i21 < 0 || i22 < 0 || i23 < 0 || i21 >= 64 || i22 >= 64 || i23 >= 64)
                                    break;
                                
                                int i24 = i21 + i22 * 64 + i23 * 4096;
                                int i25 = world[i24];
                                
                                if (i25 > 0) {
                                    i6 = (int)((rayX + rayZ) * 16.0F) & 0xF;
                                    i7 = ((int)(rayY * 16.0F) & 0xF) + 16;
                                    
                                    if (axis == AXIS_Y) {
                                        i6 = (int)(rayX * 16.0F) & 0xF;
                                        i7 = (int)(rayZ * 16.0F) & 0xF;
                                        
                                        if (rayDeltaY < 0.0F)
                                            i7 += 32;
                                    }
                                    
                                    int i26 = 0xFFFFFF; // white
                                    if (i24 != hoveredBlock || (i6 > 0 && i7 % 16 > 0 && i6 < 15 && i7 % 16 < 15))
                                        i26 = textureAtlas[i6 + i7 * 16 + i25 * 256 * 3];
                                    
                                    if (rayTravelDist < playerReach && screenX == (SCR_WIDTH * 2) / 4 && screenY == (SCR_HEIGHT * 2) / 4) {
                                        newHoveredBlock = i24;
                                        placeBlockOffset = 1;
                                        if (delta > 0.0F)
                                            placeBlockOffset = -1;
                                        
                                        placeBlockOffset *= Math.pow(WORLD_SIZE, axis);
                                        playerReach = rayTravelDist;
                                    }
                                    
                                    if (i26 > 0) {
                                        i16 = i26;
                                        fogIntensity = 0xFF - (int)(rayTravelDist / 20.0F * 0xFF);
                                        fogIntensity = fogIntensity * (0xFF - (axis + 2) % 3 * 50) / 0xFF;
                                        renderDistSorta = rayTravelDist;
                                    }
                                }
                                
                                // xyz?
                                rayX += rayDeltaX;
                                rayY += rayDeltaY;
                                rayZ += rayDeltaZ;
                                
                                rayTravelDist += f28;
                            }
                        }
                        
                        int pixelR = (i16 >> 16 & 0xFF) * fogIntensity / 0xFF;
                        int pixelG = (i16 >> 8  & 0xFF) * fogIntensity / 0xFF;
                        int pixelB = (i16       & 0xFF) * fogIntensity / 0xFF;
                        
                        screenBuffer[screenX + screenY * SCR_WIDTH] = pixelR << 16 | pixelG << 8 | pixelB;
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
    public void paint(java.awt.Graphics g)
    {
        g.drawImage(screen, 0, 0, WINDOW_WIDTH, WINDOW_HEIGHT, null);
    }
}

class MinecraftEventListener extends java.awt.event.KeyAdapter implements java.awt.event.MouseListener, java.awt.event.MouseMotionListener
{
    @Override
    public void keyPressed(KeyEvent e) {
        input[e.getKeyCode()] = 1;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        input[e.getKeyCode()] = 0;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        mouseMoved(e);
        
        if (e.isMetaDown()) {
            input[MOUSE_LEFT] = 1;
            return;
        }
        input[MOUSE_RIGHT] = 1;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isMetaDown()) {
            input[MOUSE_LEFT] = 0;
            return;
        }
        input[MOUSE_RIGHT] = 0;
    }

    @Override
    public void mouseExited(MouseEvent e) {
        mouseDelta = new Point();
    }
    
    // mouse movement stuff
    boolean recentering = true;
    Point mouseLocation = new Point();
    
    java.awt.Robot robot;
    private void recenterMouse(JFrame frame) {
        // create Robot
        if(robot == null) {
            try {
                robot = new java.awt.Robot();
            } catch (java.awt.AWTException ex) {
                ex.printStackTrace();
            }
        }
        
        if (robot != null && frame.isShowing()) {
            Point frameCenter = new Point();
            frameCenter.x = frame.getWidth() / 2;
            frameCenter.y = frame.getHeight() / 2;
            SwingUtilities.convertPointToScreen(frameCenter, frame);
            
            recentering = true;
            robot.mouseMove(frameCenter.x, frameCenter.y);
        }
    }
    
    @Override
    public void mouseMoved(MouseEvent e) {
        // this event is from re-centering the mouse - ignore it
        if (recentering)
        {
            SwingUtilities.invokeLater(() -> recentering = false);
        } else {
            mouseDelta.x = e.getX() - mouseLocation.x;
            mouseDelta.y = e.getY() - mouseLocation.y;
            
            if(mouseDelta.distanceSq(new Point()) <= 2) // looks like Robot doesn't exactly recenter.. we can get drift
                mouseDelta = new Point();
            
            // recenter the mouse
            recenterMouse((JFrame) e.getSource());
        }
        
        mouseLocation.x = e.getX();
        mouseLocation.y = e.getY();
    }
    
    @Override
    public void mouseEntered(MouseEvent e) {
        recenterMouse((JFrame) e.getSource());
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseDragged(MouseEvent e) {}
}