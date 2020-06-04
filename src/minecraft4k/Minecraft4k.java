package minecraft4k;

import java.awt.Color;
import java.awt.Font;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JPanel;

import static minecraft4k.Minecraft4k.*;

public class Minecraft4k
    extends JPanel
{
    static JFrame frame;
    
    static boolean[] input = new boolean[32767];
    
    static Point mouseDelta = new Point();
    volatile static long lastMouseMove = System.currentTimeMillis();
    
    volatile static boolean needsResUpdate = true;
    
    final static int MOUSE_RIGHT = 0;
    final static int MOUSE_LEFT = 1;
    
    static int SCR_DETAIL = 1;
    
    static int SCR_RES_X = (int) (107 * Math.pow(2, SCR_DETAIL));
    static int SCR_RES_Y = (int) (60 * Math.pow(2, SCR_DETAIL));
    
    final static float RENDER_DIST = 20.0f;
    
    final static int THREADS = 6;
    static ArrayList<RenderThread> threadList = new ArrayList();
    
    final static int WINDOW_WIDTH = 856;
    final static int WINDOW_HEIGHT = 480;
    
    final static int TEXTURE_RES = 16;
    
    final static int WORLD_SIZE = 64;
    final static int WORLD_HEIGHT = WORLD_SIZE;
    
    final static int AXIS_X = 0;
    final static int AXIS_Y = 1;
    final static int AXIS_Z = 2;
    
    final static int BLOCK_AIR = 0;
    final static int BLOCK_GRASS = 1;
    final static int BLOCK_STONE = 4;
    final static int BLOCK_BRICKS = 5;
    final static int BLOCK_WOOD = 7;
    final static int BLOCK_LEAVES = 8;
    
    final static int PERLIN_RES = 1024;
    
    static BufferedImage crosshair;
    final static int CROSS_SIZE = 32;
    
    static long deltaTime = 0;
    static Font font = Font.getFont("Arial");
    
    public static void main(String[] args)
    {
        frame = new JFrame("Minecraft4k");
        Minecraft4k m4k = new Minecraft4k();
        
        crosshair = new BufferedImage(CROSS_SIZE, CROSS_SIZE, BufferedImage.TYPE_INT_ARGB);
        
        for(int x = 0; x < CROSS_SIZE; x++)
        {
            for(int y = 0; y < CROSS_SIZE; y++)
            {
                if(((Math.abs(x - CROSS_SIZE / 2) + 2) * (Math.abs(y - CROSS_SIZE / 2) + 2) < Math.sqrt((CROSS_SIZE * CROSS_SIZE / 2)))
                        && (Math.abs(x - CROSS_SIZE / 2) + 2) + (Math.abs(y - CROSS_SIZE / 2) + 2) < CROSS_SIZE * 0.4375f
                        && (Math.abs(x - CROSS_SIZE / 2) + 2) + (Math.abs(y - CROSS_SIZE / 2) + 2) > CROSS_SIZE * 0.296875f)
                    crosshair.setRGB(x, y, 0xFFFFFFFF);
            }
        }
        
        java.util.Random localRand = new java.util.Random();
        for(int x = 0; x < PERLIN_RES; x++) {
            for(int y = 0; y < PERLIN_RES; y++) {
                float vecX = localRand.nextFloat();
                float vecY = localRand.nextFloat();
                
                // normalize
                double len = Math.sqrt(vecX * vecX + vecY * vecY);
                vecX /= len;
                vecY /= len;
                
                PERLIN_VECTORS[x][y][0] = vecX;
                PERLIN_VECTORS[x][y][1] = vecY;
            }
        }
        
        updateScreenResolution();
        
        frame.addMouseListener(new MinecraftEventListener());
        frame.addMouseMotionListener(new MinecraftEventListener());
        frame.addKeyListener(new MinecraftEventListener());
        
        frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
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

    static BufferedImage SCREEN = new BufferedImage(SCR_RES_X, SCR_RES_Y, 1);
    
    static float playerX = 96.5f;
    static float playerY = WORLD_HEIGHT + 1; // as y -> inf, player -> down
    static float playerZ = 96.5f;

    static float velocityX = 0.0f;
    static float velocityY = 0.0f;
    static float velocityZ = 0.0f;
    
    // mouse movement stuff
    //volatile static boolean recentering = true;
    volatile static Point mouseLocation = new Point();
    volatile static Point lastMouseLocation = new Point();
    volatile static boolean hovered = false;
    
    volatile static int hoveredBlockIndex = -1; // index in world array
    volatile static int placeBlockOffset = 0; // offset to hoveredBlockIndex to find where a block will be placed

    static float cameraYaw = 0.0f;
    static float cameraPitch = 0.0f;
    static float FOV = 90.0f;
    
    static float sinYaw, sinPitch;
    static float cosYaw, cosPitch;

    volatile static float newHoveredBlock = -1.0f;
    
    final static float[][][] PERLIN_VECTORS = new float[PERLIN_RES][PERLIN_RES][2];
    
    static int[] screenBuffer = ((DataBufferInt) SCREEN.getRaster().getDataBuffer()).getData();
    static int[] world = new int[WORLD_SIZE * WORLD_HEIGHT * WORLD_SIZE];
    static int[] textureAtlas = new int[16 * 3 * TEXTURE_RES * TEXTURE_RES];
    
    boolean classic = false;
    
    public void run() {
        try {
            java.util.Random rand = new java.util.Random(18295169L);
            
            // fill world with random blocks
            for (int x = 0; x < WORLD_SIZE; x++) {
                for(int y = 0; y < WORLD_HEIGHT; y++) {
                    for(int z = 0; z < WORLD_SIZE; z++) {
                        int block;
                        
                        if(classic) {
                            if(y > 32 + rand.nextInt(8))
                                block = (rand.nextInt(8) + 1);
                            else
                                block = BLOCK_AIR;
                        } else {
                            int terrainHeight = (int) ((float) (WORLD_HEIGHT / 2) + perlin((float) (x / (float) (WORLD_HEIGHT / 2)), (float) (z / (float) (WORLD_HEIGHT / 2))) * 15.0f);
                            
                            if(y > 40)
                                block = BLOCK_STONE;
                            else if (y > terrainHeight + 1)
                                block = 2; // dirt
                            else if (y > terrainHeight)
                                block = BLOCK_GRASS;
                            else
                                block = BLOCK_AIR;
                        }

                        world[x * WORLD_SIZE * WORLD_SIZE + y * WORLD_HEIGHT + z] = block;
                    }
                }
            }
            
            // procedually generates the 16x3 textureAtlas with a tileSize of 16
            // gsd = grayscale detail
            for (int blockType = 1; blockType < 16; blockType++) {
                int gsd_tempA = 0xFF - rand.nextInt(96);

                for (int y = 0; y < TEXTURE_RES * 3; y++) {
                    for (int x = 0; x < TEXTURE_RES; x++) {
                        // gets executed per pixel/texel
                        
                        if (blockType != BLOCK_STONE || rand.nextInt(3) == 0) // if the block type is stone, update the noise value less often to get a streched out look
                            gsd_tempA = 0xFF - rand.nextInt(96);
                        
                        int tint = 0x966C4A; // brown (dirt)
                        switch(blockType)
                        {
                            case BLOCK_STONE:
                                tint = 0x7F7F7F; // grey
                                break;
                            case BLOCK_GRASS:
                                if (y < (x * x * 3 + x * 81 >> 2 & 0x3) + (float) (TEXTURE_RES * 1.125f)) // grass + grass edge
                                    tint = 0x6AAA40; // green
                                else if (y < (x * x * 3 + x * 81 >> 2 & 0x3) + (float) (TEXTURE_RES * 1.1875f)) // grass edge shadow
                                    gsd_tempA = gsd_tempA * 2 / 3;
                                break;
                            case BLOCK_WOOD:
                                tint = 0x675231; // brown (bark)
                                if(!(y >= TEXTURE_RES && y < TEXTURE_RES * 2) && // avoid this block when we are on second row
                                        x > 0 && x < TEXTURE_RES - 1 &&
                                        ((y > 0 && y < TEXTURE_RES - 1) || (y > TEXTURE_RES * 2 && y < TEXTURE_RES * 3 - 1))) { // wood side area
                                    tint = 0xBC9862; // light brown

                                    // the following code repurposes 2 gsd variables making it a bit hard to read
                                    // but in short it gets the absulte distance from the tile's center in x and y direction 
                                    // finds the max of it
                                    // uses that to make the gray scale detail darker if the current pixel is part of an annual ring
                                    // and adds some noice as a finishig touch
                                    int centerThingy = TEXTURE_RES / 2 - 1;
                                    
                                    int gsd_final = x - centerThingy;
                                    int gsd_tempB = (y % TEXTURE_RES) - centerThingy;

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
                        if (y >= TEXTURE_RES * 2) // bottom side of the block
                            gsd_final /= 2; // has to be darker

                        if (blockType == BLOCK_LEAVES) {
                            tint = 0x50D937; // green
                            if (rand.nextInt(2) == 0) {
                                tint = 0;
                                gsd_final = 0xFF;
                            }
                        }
                        
                        // multiply tint by the grayscale detail
                        int col = (tint >> 16 & 0xFF) * gsd_final / 0xFF << 16 |
                                  (tint >>  8 & 0xFF) * gsd_final / 0xFF << 8 | 
                                  (tint       & 0xFF) * gsd_final / 0xFF;

                        // write pixel to the texture atlas
                        textureAtlas[x + y * TEXTURE_RES + blockType * (TEXTURE_RES * TEXTURE_RES) * 3] = col;
                    }
                }
            }
            
            long startTime = System.currentTimeMillis();
            
            
            while (true) {
                long time = System.currentTimeMillis();
                
                if(needsResUpdate) {
                    needsResUpdate = false;
                    updateScreenResolution();
                }
                
                if(input[KeyEvent.VK_Q] == true)
                {
                    System.out.println("DEBUG::BREAK");
                }
                
                sinYaw = (float)Math.sin(cameraYaw);
                cosYaw = (float)Math.cos(cameraYaw);
                sinPitch = (float)Math.sin(cameraPitch);
                cosPitch = (float)Math.cos(cameraPitch);
                
                while (System.currentTimeMillis() - startTime > 10L) {
                    // adjust camera
                    cameraYaw += mouseDelta.x / 400.0F;
                    cameraPitch -= mouseDelta.y / 400.0F;

                    if (cameraPitch < -1.57F)
                        cameraPitch = -1.57F;

                    if (cameraPitch > 1.57F)
                        cameraPitch = 1.57F;
                    
                    
                    startTime += 10L;
                    float inputX = (integer(input[KeyEvent.VK_D]) - integer(input[KeyEvent.VK_A])) * 0.02F;
                    float inputZ = (integer(input[KeyEvent.VK_W]) - integer(input[KeyEvent.VK_S])) * 0.02F;
                    velocityX *= 0.5F;
                    velocityY *= 0.99F;
                    velocityZ *= 0.5F;
                    velocityX += sinYaw * inputZ + cosYaw * inputX;
                    velocityZ += cosYaw * inputZ - sinYaw * inputX;
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
                                    || world[colliderBlockX + colliderBlockY * WORLD_HEIGHT + colliderBlockZ * (WORLD_SIZE * WORLD_SIZE)] > BLOCK_AIR) {
                                
                                if (axisIndex != AXIS_Z) //not checking for vertical movement
                                    continue OUTER; //movement is invalid
                                
                                // if we're falling, colliding, and we press space
                                if (input[KeyEvent.VK_SPACE] == true && velocityY > 0.0F) {
                                    input[KeyEvent.VK_SPACE] = false;
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
                
                // break block
                if (input[MOUSE_LEFT] == true && hoveredBlockIndex > 0) {
                    world[hoveredBlockIndex] = BLOCK_AIR;
                    input[MOUSE_LEFT] = false;
                }
                
                // place block
                if (input[MOUSE_RIGHT] == true && hoveredBlockIndex > 0) {
                    world[hoveredBlockIndex + placeBlockOffset] = BLOCK_GRASS;
                    input[MOUSE_RIGHT] = false;
                }
                
                for (int i8 = 0; i8 < 12; i8++) {
                    int magicX = (int)(playerX + (i8 >> 0 & 1) * 0.6F - 0.3F) - WORLD_SIZE;
                    int magicY = (int)(playerY + ((i8 >> 2) - 1) * 0.8F + 0.65F) - WORLD_HEIGHT;
                    int magicZ = (int)(playerZ + (i8 >> 1 & 1) * 0.6F - 0.3F) - WORLD_SIZE;
                    
                    // check if hovered block is within world boundaries
                    if (magicX >= 0 && magicY >= 0 && magicZ >= 0 && magicX < WORLD_SIZE && magicY < WORLD_HEIGHT && magicZ < WORLD_SIZE)
                        world[magicX + magicY * WORLD_HEIGHT + magicZ * (WORLD_SIZE * WORLD_SIZE)] = BLOCK_AIR;
                }
                
                // render the SCREEN
                newHoveredBlock = -1.0F;
                
                for(RenderThread t : threadList)
                    t.render = true;
                
                boolean done = false;
                while(!done) {
                    done = true;
                    for(RenderThread t : threadList) {
                        if(t.render) {
                            done = false;
                            break;
                        }
                    }
                    
                    if(done)
                        break;
                }
                
                hoveredBlockIndex = (int) newHoveredBlock;
                
                deltaTime = System.currentTimeMillis() - time;
                
                for(RenderThread t : threadList)
                    System.arraycopy(t.buffer, 0, screenBuffer, t.start, t.buffer.length);
                
                // reset mouse delta so if we stop moving the mouse it doesn't drift
                if(System.currentTimeMillis() - lastMouseMove > 25)
                    mouseDelta = new Point();
                
                Thread.sleep(2);
                
                repaint();
                
                if(hovered) {
                    mouseLocation = MouseInfo.getPointerInfo().getLocation();
                    
                    mouseDelta = new Point(mouseLocation.x - lastMouseLocation.x, mouseLocation.y - lastMouseLocation.y);
                    
                    recenterMouse(frame);
                    
                    mouseLocation = MouseInfo.getPointerInfo().getLocation();
                    lastMouseLocation = mouseLocation;
                } else {
                    mouseDelta = new Point();
                }
            }
        } catch (Exception localException) {
            localException.printStackTrace();
        }
    }
    
    volatile java.awt.Robot robot;
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
            javax.swing.SwingUtilities.convertPointToScreen(frameCenter, frame);
            
            //recentering = true;
            robot.mouseMove(frameCenter.x, frameCenter.y);
        }
    }
    
     // Compute Perlin noise at coordinates x, y
    static float perlin(float x, float y) {

        // Determine grid cell coordinates
        int x0 = (int) x;
        int x1 = x0 + 1;
        int y0 = (int) y;
        int y1 = y0 + 1;

        // Determine interpolation weights
        // Could also use higher order polynomial/s-curve here
        float sx = x - (float)x0;
        float sy = y - (float)y0;

        // Interpolate between grid point gradients
        float n0, n1, ix0, ix1, value;
        n0 = dotGridGradient(x0, y0, x, y);
        n1 = dotGridGradient(x1, y0, x, y);
        ix0 = lerp(n0, n1, sx);
        n0 = dotGridGradient(x0, y1, x, y);
        n1 = dotGridGradient(x1, y1, x, y);
        ix1 = lerp(n0, n1, sx);
        value = lerp(ix0, ix1, sy);

        return value;
    }
    
    // Function to linearly interpolate between a0 and a1
    // Weight w should be in the range [0.0, 1.0]
    static float lerp(float a0, float a1, float w) {
        return (1.0f - w) * a0 + w * a1;
    }

    // Computes the dot product of the distance and gradient vectors.
    static float dotGridGradient(int ix, int iy, float x, float y) {
        // Compute the distance vector
        float dx = x - (float)ix;
        float dy = y - (float)iy;

        // Compute the dot-product
        return (dx * PERLIN_VECTORS[iy][ix][0] + dy * PERLIN_VECTORS[iy][ix][1]);
    }
    
    @Override
    public void paint(java.awt.Graphics g)
    {
        g.drawImage(SCREEN, 0, 0, WINDOW_WIDTH, WINDOW_HEIGHT, null);
        
        g.drawImage(crosshair, WINDOW_WIDTH / 2 - CROSS_SIZE / 2, WINDOW_HEIGHT / 2 - CROSS_SIZE / 2, null);
        
        if(deltaTime > 16)
            g.setColor(Color.red);
        else
            g.setColor(Color.white);
        
        g.setFont(font);
        g.drawString("" + deltaTime, 0, 10);
    }
    
    public static void updateScreenResolution()
    {
        SCR_RES_X = (int) (107 * Math.pow(2, SCR_DETAIL));
        SCR_RES_Y = (int) (60  * Math.pow(2, SCR_DETAIL));
        
        SCREEN = new BufferedImage(SCR_RES_X, SCR_RES_Y, 1);
        screenBuffer = ((DataBufferInt) SCREEN.getRaster().getDataBuffer()).getData();
        
        threadList.clear();
        
        int start = 0;
        for(int fragmentID = 0; fragmentID < THREADS; fragmentID++)
        {
            int end = start + screenBuffer.length / THREADS;
            
            RenderThread t  = new RenderThread(start, end);
            threadList.add(t);
            new Thread(t).start();
            
            start = end;
        }
        
        // auto generated code - do not delete
        String title = "Minecraft4k";
        
        switch(SCR_RES_X) {
            case 6:
                title += " on battery-saving mode";
                break;
            case 13:
                title += " on a potato";
                break;
            case 26:
                title += " on an undocked switch";
                break;
            case 53:
                title += " on a TI-84";
                break;
            case 107:
                title += " on an Atari 2600";
                break;
            case 428:
                title += " at SD";
                break;
            case 856:
                title += " at HD";
                break;
            case 1712:
                title += " at Full HD";
                break;
            case 3424:
                title += " at 4K";
                break;
            case 6848:
                title += " on a NASA supercomputer";
        }
        
        frame.setTitle(title);
    }
    
    // ew java
    private static int integer(boolean b)
    {
        return b ? 1 : 0;
    }
}

class MinecraftEventListener extends java.awt.event.KeyAdapter implements java.awt.event.MouseListener, java.awt.event.MouseMotionListener
{
    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_PERIOD:
                SCR_DETAIL++;
                needsResUpdate = true;
                break;
            case KeyEvent.VK_COMMA:
                SCR_DETAIL--;
                if(SCR_RES_Y <= 1)
                    SCR_DETAIL++;
                needsResUpdate = true;
                break;
            default:
                input[e.getKeyCode()] = true;
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        input[e.getKeyCode()] = false;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            input[MOUSE_LEFT] = true;
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            input[MOUSE_RIGHT] = true;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            input[MOUSE_LEFT] = false;
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            input[MOUSE_RIGHT] = false;
        }
    }
    
    @Override
    public void mouseEntered(MouseEvent e) {
        hovered = ((JFrame) e.getSource()).isFocused();
    }
    
    @Override
    public void mouseExited(MouseEvent e) {
        hovered = ((JFrame) e.getSource()).isFocused();
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseDragged(MouseEvent e) {}

    @Override
    public void mouseMoved(MouseEvent e) {}
}

class RenderThread implements Runnable {
    int start, end;
    RenderThread(int _start, int _end)
    {
        start = _start;
        end = _end;
        
        buffer = new int[end - start];
    }
    
    int[] buffer;

    @Override
    public void run()
    {
        while(true) {
            for(int screenIndex = 0; screenIndex < buffer.length; screenIndex++)
            {
                int screenX = (screenIndex + start) % SCR_RES_X;
                int screenY = (screenIndex + start) / SCR_RES_X;

                float xDistSmall = ((screenX - (SCR_RES_X / 2)) / FOV) / (float) SCR_RES_X * 214.0f;
                float yDistSmall = ((screenY - (SCR_RES_Y / 2)) / FOV) / (float) SCR_RES_Y * 120.0f;

                float temp = cosPitch + yDistSmall * sinPitch;

                float rayDirX = xDistSmall * cosYaw + temp * sinYaw;
                float rayDirY = yDistSmall * cosPitch - sinPitch;
                float rayDirZ = temp * cosYaw - xDistSmall * sinYaw;

                int pixelColor = 0;
                int fogMultipliter = 0x00;
                double furthestHit = RENDER_DIST;
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

                    float rayDeltaX = rayDirX / Math.abs(delta);
                    float rayDeltaY = rayDirY / Math.abs(delta);
                    float rayDeltaZ = rayDirZ / Math.abs(delta);


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

                    float rayTravelDist = floatComponent / Math.abs(delta);

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

                    while (rayTravelDist < furthestHit) {
                        int blockHitX = (int) rayX - WORLD_SIZE;
                        int blockHitY = (int) rayY - WORLD_HEIGHT;
                        int blockHitZ = (int) rayZ - WORLD_SIZE;

                        if (blockHitX < 0 || blockHitY < 0 || blockHitZ < 0 || blockHitX >= WORLD_SIZE || blockHitY >= WORLD_SIZE || blockHitZ >= WORLD_SIZE)
                            break;

                        int blockHitIndex = blockHitX + blockHitY * WORLD_HEIGHT + blockHitZ * (WORLD_SIZE * WORLD_SIZE);
                        int blockHitID = world[blockHitIndex];

                        if (blockHitID != BLOCK_AIR) {
                            int texFetchX = (int)((rayX + rayZ) * TEXTURE_RES) % TEXTURE_RES;
                            int texFetchY = ((int)(rayY * TEXTURE_RES) % TEXTURE_RES) + TEXTURE_RES;

                            if (axis == AXIS_Y) {
                                texFetchX = (int)(rayX * TEXTURE_RES) % TEXTURE_RES;
                                texFetchY = (int)(rayZ * TEXTURE_RES) % TEXTURE_RES;

                                // "lighting"
                                if (rayDeltaY < 0.0F)
                                    texFetchY += TEXTURE_RES * 2;
                            }

                            int textureColor;
                            if(blockHitIndex == hoveredBlockIndex &&
                                    (  (texFetchX == 0               || texFetchY % TEXTURE_RES == 0)
                                    || (texFetchX == TEXTURE_RES - 1 || texFetchY % TEXTURE_RES == TEXTURE_RES - 1)))
                                textureColor = 0xFFFFFF; // add white outline to hovered block
                            else
                                textureColor = textureAtlas[texFetchX + texFetchY * TEXTURE_RES + blockHitID * (TEXTURE_RES * TEXTURE_RES) * 3];

                            if (rayTravelDist < playerReach && screenX == (SCR_RES_X * 2) / 4 && screenY == (SCR_RES_Y * 2) / 4) {
                                newHoveredBlock = blockHitIndex;
                                placeBlockOffset = 1;
                                if (delta > 0.0F)
                                    placeBlockOffset = -1;

                                placeBlockOffset *= Math.pow(WORLD_SIZE, axis);
                                playerReach = rayTravelDist;
                            }

                            if (textureColor > 0) {
                                pixelColor = textureColor;
                                fogMultipliter = 0xFF - (int)(rayTravelDist / RENDER_DIST * 0xFF);
                                fogMultipliter = fogMultipliter * (0xFF - (axis + 2) % 3 * 50) / 0xFF;
                                furthestHit = rayTravelDist;
                            }
                        }

                        rayX += rayDeltaX;
                        rayY += rayDeltaY;
                        rayZ += rayDeltaZ;

                        rayTravelDist += 1.0f / Math.abs(delta);
                    }
                }

                int pixelR = (pixelColor >> 16 & 0xFF) * fogMultipliter / 0xFF;
                int pixelG = (pixelColor >> 8  & 0xFF) * fogMultipliter / 0xFF;
                int pixelB = (pixelColor       & 0xFF) * fogMultipliter / 0xFF;

                buffer[screenIndex] = pixelR << 16 | pixelG << 8 | pixelB;
            }

            render = false;
            while(!render) {
                try {
                    Thread.sleep(8);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            } // stuck here until render = true
        }
    }
    
    volatile boolean render = true;
}