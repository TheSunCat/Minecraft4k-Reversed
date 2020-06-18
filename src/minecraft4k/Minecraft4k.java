package minecraft4k;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Random;
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
    
    final static int THREADS = Runtime.getRuntime().availableProcessors();
    static ArrayList<RenderThread> threadList = new ArrayList();
    
    final static int WINDOW_WIDTH = 856;
    final static int WINDOW_HEIGHT = 480;
    
    final static int TEXTURE_RES = 16;
    
    final static int WORLD_SIZE = 64;
    final static int WORLD_HEIGHT = 64;
    
    final static int AXIS_X = 0;
    final static int AXIS_Y = 1;
    final static int AXIS_Z = 2;
    
    final static int BLOCK_AIR = 0;
    final static int BLOCK_GRASS = 1;
    final static int BLOCK_DEFAULT_DIRT = 2;
    final static int BLOCK_STONE = 4;
    final static int BLOCK_BRICKS = 5;
    final static int BLOCK_WOOD = 7;
    final static int BLOCK_LEAVES = 8;
    
    final static int PERLIN_RES = 1024;
    
    static BufferedImage crosshair;
    final static int CROSS_SIZE = 32;
    
    static long deltaTime = 0;
    static Font font = Font.getFont("Arial");
    static java.awt.Cursor hiddenCursor;
    
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
        
        updateScreenResolution();
        
        frame.addMouseListener(new MinecraftEventListener());
        frame.addMouseMotionListener(new MinecraftEventListener());
        frame.addKeyListener(new MinecraftEventListener());
        frame.addFocusListener(new MinecraftEventListener());
        frame.addMouseWheelListener(new MinecraftEventListener());
        
        frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null); // center the window
        
        hiddenCursor = frame.getToolkit().createCustomCursor(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), new java.awt.Point(), null);
        
        // hide the cursor
        frame.setCursor(hiddenCursor);
        
        // add Minecraft!
        frame.getContentPane().add(m4k);
        
        frame.setVisible(true);
        m4k.run();
    }
    
    float perlin_octaves = 4; // default to medium smooth
    float perlin_amp_falloff = 0.5f; // 50% reduction/octave
    int PERLIN_YWRAPB = 4;
    int PERLIN_YWRAP = 1 << PERLIN_YWRAPB;
    int PERLIN_ZWRAPB = 8;
    int PERLIN_ZWRAP = 1 << PERLIN_ZWRAPB;
    
    
    float scaled_cosine(float i) {
        return (float) (0.5f * (1.0f - Math.cos(i * Math.PI)));
    }
    
    float noise(float x, float y) { // stolen from Processing
        if (perlin == null) {
            Random r = new Random(18295169L);
            
            perlin = new float[PERLIN_RES + 1];
            for (int i = 0; i < PERLIN_RES + 1; i++) {
                perlin[i] = r.nextFloat();
            }
        }

        if (x < 0)
            x = -x;
        if (y < 0)
            y = -y;
        
        int xi = (int) x;
        int yi = (int) y;
        
        float xf = x - xi;
        float yf = y - yi;
        float rxf, ryf;

        float r = 0;
        float ampl = 0.5f;

        float n1, n2, n3;

        for (int i = 0; i < perlin_octaves; i++) {
            int of = xi + (yi << PERLIN_YWRAPB);
            
            rxf = scaled_cosine(xf);
            ryf = scaled_cosine(yf);
            
            n1 = perlin[of % PERLIN_RES];
            n1 += rxf * (perlin[(of + 1) % PERLIN_RES] - n1);
            n2 = perlin[(of + PERLIN_YWRAP) % PERLIN_RES];
            n2 += rxf * (perlin[(of + PERLIN_YWRAP + 1) % PERLIN_RES] - n2);
            n1 += ryf * (n2 - n1);

            of += PERLIN_ZWRAP;
            n2 = perlin[of % PERLIN_RES];
            n2 += rxf * (perlin[(of + 1) % PERLIN_RES] - n2);
            n3 = perlin[(of + PERLIN_YWRAP) % PERLIN_RES];
            n3 += rxf * (perlin[(of + PERLIN_YWRAP + 1) % PERLIN_RES] - n3);
            n2 += ryf * (n3 - n2);

            n1 += scaled_cosine(0) * (n2 - n1);

            r += n1 * ampl;
            ampl *= perlin_amp_falloff;
            xi <<= 1;
            xf *= 2;
            yi <<= 1;
            yf *= 2;

            if (xf >= 1.0) {
                xi++;
                xf--;
            }
            
            if (yf >= 1.0) {
                yi++;
                yf--;
            }
        }
        
        return r;
    }

    static BufferedImage SCREEN = new BufferedImage(SCR_RES_X, SCR_RES_Y, 1);
    
    static float playerX = WORLD_SIZE + WORLD_SIZE / 2 + 0.5f;
    static float playerY = WORLD_HEIGHT + 1; // more y means more down
    static float playerZ = playerX;

    static float velocityX = 0.0f;
    static float velocityY = 0.0f;
    static float velocityZ = 0.0f;
    
    // mouse movement stuff
    volatile static Point mouseLocation = new Point();
    volatile static Point lastMouseLocation = new Point();
    volatile static boolean hovered = false;
    
    volatile static int hoveredBlockPosX = -1;
    volatile static int hoveredBlockPosY = -1;
    volatile static int hoveredBlockPosZ = -1;
    
    volatile static int placeBlockPosX = -1;
    volatile static int placeBlockPosY = -1;
    volatile static int placeBlockPosZ = -1;
    
    volatile static int newHoverBlockPosX = -1;
    volatile static int newHoverBlockPosY = -1;
    volatile static int newHoverBlockPosZ = -1;

    static float cameraYaw = 0.0f;
    static float cameraPitch = 0.0f;
    static float FOV = 90.0f;
    
    static float sinYaw, sinPitch;
    static float cosYaw, cosPitch;
    
    static float[] perlin = null;
    
    static int[] screenBuffer = ((DataBufferInt) SCREEN.getRaster().getDataBuffer()).getData();
    static byte[][][] world = new byte[WORLD_SIZE][WORLD_HEIGHT][WORLD_SIZE];
    
    static BufferedImage textureAtlasImage = new BufferedImage(TEXTURE_RES, 3 * 16 * TEXTURE_RES, BufferedImage.TYPE_INT_ARGB);
    static int[] textureAtlas = ((DataBufferInt) textureAtlasImage.getRaster().getDataBuffer()).getData();
    
    static byte[] hotbar = new byte[] { BLOCK_GRASS, BLOCK_DEFAULT_DIRT, BLOCK_STONE, BLOCK_BRICKS, BLOCK_WOOD, BLOCK_LEAVES };
    static int heldBlockIndex = 0;
    
    boolean classic = true;
    
    public void run() {
        try {
            Random rand = new Random(18295169L);
            
            // generate world
            for (int x = WORLD_SIZE; x >= 0; x--) {
                for(int y = 0; y < WORLD_HEIGHT; y++) {
                    for(int z = 0; z < WORLD_SIZE; z++) {
                        byte block;
                        
                        float maxTerrainHeight = WORLD_HEIGHT / 2f;
                        
                        if(classic) {
                            if(y > maxTerrainHeight + rand.nextInt(8))
                                block = (byte) (rand.nextInt(8) + 1);
                            else
                                block = BLOCK_AIR;
                        } else {
                            float halfWorldSize = WORLD_SIZE / 2f;
                            
                            int terrainHeight = Math.round(maxTerrainHeight + noise(x / halfWorldSize, z / halfWorldSize) * 10.0f);
                            
                            if(y > WORLD_HEIGHT * 0.75f)
                                block = BLOCK_STONE;
                            else if (y > terrainHeight + 1)
                                block = 2; // dirt
                            else if (y > terrainHeight)
                                block = BLOCK_GRASS;
                            else
                                block = BLOCK_AIR;
                        }
                        
                        if(x == WORLD_SIZE)
                            continue;
                        
                        world[x][y][z] = block;
                    }
                }
            }
            
            // set random seed to generate textures
            rand.setSeed(151910774187927L);
            
            // procedually generates the 16x3 textureAtlas
            // gsd = grayscale detail
            for (int blockType = 1; blockType < 16; blockType++) {
                int gsd_tempA = 0xFF - rand.nextInt(0x60);

                for (int y = 0; y < TEXTURE_RES * 3; y++) {
                    for (int x = 0; x < TEXTURE_RES; x++) {
                        // gets executed per pixel/texel
                        
                        int gsd_final;
                        int tint;
                        
                        if(classic) {
                            if (blockType != BLOCK_STONE || rand.nextInt(3) == 0) // if the block type is stone, update the noise value less often to get a streched out look
                                gsd_tempA = 0xFF - rand.nextInt(0x60);

                            tint = 0x966C4A; // brown (dirt)
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
                                        int woodCenter = TEXTURE_RES / 2 - 1;
                                        int gsd_wood = x - woodCenter;
                                        int gsd_tempB = (y % TEXTURE_RES) - woodCenter;

                                        if (gsd_wood < 0)
                                            gsd_wood = 1 - gsd_wood;

                                        if (gsd_tempB < 0)
                                            gsd_tempB = 1 - gsd_tempB;

                                        if (gsd_tempB > gsd_wood)
                                            gsd_wood = gsd_tempB;

                                        gsd_tempA = 196 - rand.nextInt(32) + gsd_wood % 3 * 32;
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

                            gsd_final = gsd_tempA;
                            if (y >= TEXTURE_RES * 2) // bottom side of the block
                                gsd_final /= 2; // has to be darker

                            if (blockType == BLOCK_LEAVES) {
                                tint = 0x50D937; // green
                                if (rand.nextInt(2) == 0) {
                                    tint = 0;
                                    gsd_final = 0xFF;
                                }
                            }
                        } else {
                            float pNoise = noise(x, y);

                            tint = 0x966C4A; // brown (dirt)
                            
                            gsd_tempA = (int) ((1 - pNoise * 0.5f) * 255);
                            switch(blockType) {
                                case BLOCK_STONE:
                                    tint = 0x7F7F7F; // grey
                                    gsd_tempA = (int)((0.75 + Math.round(Math.abs(noise(x * 0.5f, y * 2))) * 0.125f) * 255);
                                    break;
                                case BLOCK_GRASS:
                                    if(y < (((x * x * 3 + x * 81) / 2) % 4) + 18) // grass + grass edge
                                        tint = 0x7AFF40; //green
                                    else if (y < ( ((x * x * 3 + x * 81)/2) % 4) + 19)
                                        gsd_tempA = gsd_tempA * 1 / 3;
                                    break;
                                case BLOCK_WOOD:
                                    tint = 0x776644; // brown (bark)

                                    float dx = Math.abs(x - 8);
                                    float dy = Math.abs((y % 16) - 8);
                                    int gsd_wood = (int) (Math.sqrt(dx * dx + dy * dy) * .25f + Math.max(dx, dy) * .75f);

                                    if(y < 16 || y > 32){
                                      if (gsd_wood < 7) { // wood inside area
                                        tint = 0xCCAA77; // light brown

                                        gsd_tempA = 196 - (int)rand.nextInt(32) + gsd_wood % 3 * 32;
                                      } else if(dx>dy){
                                        gsd_tempA = (int)(noise(y, x * .25f) * 255 * (180 - Math.sin(x * Math.PI) * 50) / 100);
                                      } else {
                                        gsd_tempA = (int)(noise(x, y * .25f) * 255 * (180 - Math.sin(x * Math.PI) * 50) / 100);
                                      }
                                    } else {
                                        gsd_tempA = (int)(noise(x, y * .25f) * 255 * (180 - Math.sin(x * Math.PI) * 50) / 100);
                                    }
                                    break;
                                case BLOCK_BRICKS:
                                    tint = 0x444444; // red

                                    float brickDX = Math.abs(x%8 - 4);
                                    float brickDY = Math.abs((y % 4) - 2) * 2;

                                    if(((int)y / 4) % 2 == 1)
                                      brickDX = Math.abs((x + 4) % 8 - 4);

                                    float d = (float) (Math.sqrt(brickDX * brickDX + brickDY * brickDY) * .5f + Math.max(brickDX, brickDY) * .5f);

                                    if (d > 4) // gap between bricks
                                        tint = 0xAAAAAA; // light grey
                                    break;
                            }

                            gsd_final = gsd_tempA;
                            if (y >= 32) // bottom side of the block
                                gsd_final /= 2; // make it darker, "sading"
                            
                            if(blockType == BLOCK_LEAVES)
                            {
                                tint = 0;

                                float dx = Math.abs(x % 4 - 2) * 2;
                                float dy = (y % 8) - 4;

                                if(((int) y / 8) % 2 == 1)
                                  dx = Math.abs((x + 2) % 4 - 2) * 2;

                                dx += pNoise;

                                float d = dx + Math.abs(dy);

                                if(dy < 0)
                                  d = (float) Math.sqrt(dx*dx + dy*dy);

                                if (d < 3.5f)
                                    tint = 0xFFCCDD;
                                else if (d < 4)
                                    tint = 0xCCAABB;
                            }
                        }
                        
                        // multiply tint by the grayscale detail
                        int col = ((tint & 0xFFFFFF) == 0 ? 0 : 0xFF)    << 24 |
                                  (tint >> 16 & 0xFF) * gsd_final / 0xFF << 16 |
                                  (tint >>  8 & 0xFF) * gsd_final / 0xFF <<  8 | 
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
                            
                            if(colliderBlockY < 0)
                                continue;
                            
                            // check collision with world bounds and world blocks
                            if (colliderBlockX < 0 || colliderBlockZ < 0
                                    || colliderBlockX >= WORLD_SIZE || colliderBlockY >= WORLD_HEIGHT || colliderBlockZ >= WORLD_SIZE
                                    || world[colliderBlockX][colliderBlockY][colliderBlockZ] != BLOCK_AIR) {
                                
                                if (axisIndex != AXIS_Z) // not checking for vertical movement
                                    continue OUTER; // movement is invalid
                                
                                // if we're falling, colliding, and we press space
                                if (input[KeyEvent.VK_SPACE] == true && velocityY > 0.0F) {
                                    velocityY = -0.1F; // jump
                                    break OUTER;
                                }
                                
                                // stop vertical movement
                                velocityY = 0.0F;
                                break OUTER;
                            }
                        }
                        
                        playerX = newPlayerX;
                        playerY = newPlayerY;
                        playerZ = newPlayerZ;
                    }
                }
                
                if(hoveredBlockPosX > -1) { // all axes will be -1 if nothing hovered
                    // break block
                    if (input[MOUSE_LEFT] == true) {
                        world[hoveredBlockPosX][hoveredBlockPosY][hoveredBlockPosZ] = BLOCK_AIR;
                        input[MOUSE_LEFT] = false;
                    }
                    
                    
                    if(placeBlockPosY > 0) {
                        // place block
                        if (input[MOUSE_RIGHT] == true) {
                            world[placeBlockPosX][placeBlockPosY][placeBlockPosZ] = hotbar[heldBlockIndex];
                            input[MOUSE_RIGHT] = false;
                        }
                    }
                }
                
                for (int colliderIndex = 0; colliderIndex < 12; colliderIndex++) {
                    int magicX = (int)(playerX + ( colliderIndex >> 0  & 1) * 0.6F - 0.3F ) - WORLD_SIZE;
                    int magicY = (int)(playerY + ((colliderIndex >> 2) - 1) * 0.8F + 0.65F) - WORLD_HEIGHT;
                    int magicZ = (int)(playerZ + ( colliderIndex >> 1  & 1) * 0.6F - 0.3F ) - WORLD_SIZE;
                    
                    // check if hovered block is within world boundaries
                    if (magicX >= 0 && magicY >= 0 && magicZ >= 0 && magicX < WORLD_SIZE && magicY < WORLD_HEIGHT && magicZ < WORLD_SIZE)
                        world[magicX][magicY][magicZ] = BLOCK_AIR;
                }
                
                // render the screen
                newHoverBlockPosX = -1;
                newHoverBlockPosY = -1;
                newHoverBlockPosZ = -1;
                
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
                
                hoveredBlockPosX = newHoverBlockPosX;
                hoveredBlockPosY = newHoverBlockPosY;
                hoveredBlockPosZ = newHoverBlockPosZ;
                
                placeBlockPosX += hoveredBlockPosX;
                placeBlockPosY += hoveredBlockPosY;
                placeBlockPosZ += hoveredBlockPosZ;
                
                deltaTime = System.currentTimeMillis() - time;
                
                for(RenderThread t : threadList)
                    System.arraycopy(t.buffer, 0, screenBuffer, t.start, t.buffer.length);
                
                // reset mouse delta so if we stop moving the mouse it doesn't drift
                if(System.currentTimeMillis() - lastMouseMove > 25)
                    mouseDelta = new Point();
                
                if(deltaTime < 14)
                    Thread.sleep(16 - deltaTime);
                
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
            
            robot.mouseMove(frameCenter.x, frameCenter.y);
        }
    }
    
    @Override
    public void paint(java.awt.Graphics g)
    {
        Graphics2D g2d = (Graphics2D) g;
        
        g2d.drawImage(SCREEN, 0, 0, WINDOW_WIDTH, WINDOW_HEIGHT, null);
        
        g2d.drawImage(crosshair, WINDOW_WIDTH / 2 - CROSS_SIZE / 2, WINDOW_HEIGHT / 2 - CROSS_SIZE / 2, null);
        
        g2d.setColor(Color.gray.darker());
        g2d.setStroke(new BasicStroke(4));
        
        final int hotbarItemSize = 64;
        final int padding = 2;
        final int hotbarX = frame.getContentPane().getWidth() / 2 - (hotbarItemSize * hotbar.length) / 2;
        final int hotbarY = frame.getContentPane().getHeight() - hotbarItemSize - 10;
        
        // draw transparent background
        g2d.setColor(new Color(0x33, 0x33, 0x33, 0x7F).darker());
        g2d.fillRect(hotbarX - 2, hotbarY - 2, hotbarItemSize * hotbar.length + 4, hotbarItemSize + 4);
        
        g2d.setColor(Color.gray.darker());
        g2d.drawRect(hotbarX - 2, hotbarY - 2, hotbarItemSize * hotbar.length + 4, hotbarItemSize + 4);
        
        for(int i = 0; i < hotbar.length; i++)
        {
            g2d.drawImage(textureAtlasImage,
                    hotbarX + i * hotbarItemSize + padding, hotbarY + padding, //draw bounds min
                    hotbarX + i * hotbarItemSize + hotbarItemSize - padding, hotbarY + hotbarItemSize - padding, //draw bounds max
                    0, TEXTURE_RES * (hotbar[i] * 3 + 1), // sample bounds min
                    TEXTURE_RES, TEXTURE_RES * (hotbar[i] * 3 + 2), // sample bounds max
                    null);
        }
        
        g2d.setColor(Color.white);
        g2d.setStroke(new BasicStroke(5));
        
        g2d.drawRect(hotbarX + heldBlockIndex * hotbarItemSize, hotbarY, hotbarItemSize, hotbarItemSize);
            
        if(deltaTime > 16 || deltaTime == 0) // 16ms = 60fps
            g2d.setColor(Color.red);
        else
            g2d.setColor(Color.white);
        
        g2d.setFont(font);
        
        String str;
        if(deltaTime == 0)
            str = "Houston, we have a problem!";
        else
            str = "" + 1000 / deltaTime + " fps, 0 chunk updates";
        
        g2d.drawString(str, 0, 10);
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

class MinecraftEventListener extends java.awt.event.KeyAdapter implements java.awt.event.MouseListener, java.awt.event.MouseMotionListener, java.awt.event.FocusListener, java.awt.event.MouseWheelListener
{
    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_PERIOD:
                SCR_DETAIL++;
                if(SCR_DETAIL > 6)
                    SCR_DETAIL = 6;
                
                needsResUpdate = true;
                break;
            case KeyEvent.VK_COMMA:
                SCR_DETAIL--;
                if(SCR_DETAIL < -4)
                    SCR_DETAIL = -4;
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
        if(frame.isFocused())
            hovered = true;
    }
    
    @Override
    public void mouseExited(MouseEvent e) {
        if(frame.isFocused())
            hovered = false;
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseDragged(MouseEvent e) {}

    @Override
    public void mouseMoved(MouseEvent e) {
        if(frame.isFocused() && frame.getBounds().contains(e.getPoint()))
            hovered = true;
    }

    @Override
    public void focusGained(FocusEvent e) {
        frame.setCursor(Minecraft4k.hiddenCursor);
    }

    @Override
    public void focusLost(FocusEvent e) {
        hovered = false;
        frame.setCursor(java.awt.Cursor.getDefaultCursor());
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        System.out.println(e.getUnitsToScroll());
        
        if(e.getUnitsToScroll() < 0)
            heldBlockIndex--;
        else
            heldBlockIndex++;
        
        if(heldBlockIndex < 0)
            heldBlockIndex = hotbar.length - 1;
        
        if(heldBlockIndex >= hotbar.length)
            heldBlockIndex = 0;
    }
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
                int fogMultiplier = 0x00;
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
                        if (axis == AXIS_X)
                            rayX--;

                        if (axis == AXIS_Y)
                            rayY--;

                        if (axis == AXIS_Z)
                            rayZ--;
                    }

                    while (rayTravelDist < furthestHit) {
                        int blockHitX = (int) rayX - WORLD_SIZE;
                        int blockHitY = (int) rayY - WORLD_HEIGHT;
                        int blockHitZ = (int) rayZ - WORLD_SIZE;

                        if (blockHitX < 0 || blockHitY < -2 || blockHitZ < 0 || blockHitX >= WORLD_SIZE || blockHitY >= WORLD_HEIGHT || blockHitZ >= WORLD_SIZE)
                            break;
                        
                        int blockHitID = blockHitY < 0 ? BLOCK_AIR : world[blockHitX][blockHitY][blockHitZ];

                        if (blockHitID != BLOCK_AIR) {
                            int texFetchX = (int)((rayX + rayZ) * TEXTURE_RES) % TEXTURE_RES;
                            int texFetchY = ((int)(rayY * TEXTURE_RES) % TEXTURE_RES) + TEXTURE_RES;

                            if (axis == AXIS_Y) {
                                texFetchX = (int)(rayX * TEXTURE_RES) % TEXTURE_RES;
                                texFetchY = (int)(rayZ * TEXTURE_RES) % TEXTURE_RES;

                                // "lighting"
                                if (rayDeltaY < 0.0F) // looking at the underside of a block
                                    texFetchY += TEXTURE_RES * 2;
                            }

                            int textureColor;
                            if(blockHitX == hoveredBlockPosX && blockHitY == hoveredBlockPosY && blockHitZ == hoveredBlockPosZ &&
                                    (  (texFetchX == 0               || texFetchY % TEXTURE_RES == 0)
                                    || (texFetchX == TEXTURE_RES - 1 || texFetchY % TEXTURE_RES == TEXTURE_RES - 1)))
                                textureColor = 0xFFFFFF; // add white outline to hovered block
                            else
                                textureColor = textureAtlas[texFetchX + texFetchY * TEXTURE_RES + blockHitID * (TEXTURE_RES * TEXTURE_RES) * 3];

                            if (rayTravelDist < playerReach && screenX == (SCR_RES_X * 2) / 4 && screenY == (SCR_RES_Y * 2) / 4) {
                                newHoverBlockPosX = blockHitX;
                                newHoverBlockPosY = blockHitY;
                                newHoverBlockPosZ = blockHitZ;
                                
                                placeBlockPosX = 0;
                                placeBlockPosY = 0;
                                placeBlockPosZ = 0;
                                
                                int direction = 1;
                                if (delta > 0.0F)
                                    direction = -1;

                                switch(axis) {
                                    case AXIS_X:
                                        placeBlockPosX = direction;
                                        break;
                                    case AXIS_Y:
                                        placeBlockPosY = direction;
                                        break;
                                    case AXIS_Z:
                                        placeBlockPosZ = direction;
                                }
                                
                                
                                playerReach = rayTravelDist;
                            }

                            if ((textureColor & 0xFFFFFF) > 0) {
                                pixelColor = textureColor;
                                fogMultiplier = 0xFF - (int)(rayTravelDist / RENDER_DIST * 0xFF);
                                fogMultiplier = fogMultiplier * (0xFF - (axis + 2) % 3 * 50) / 0xFF;
                                furthestHit = rayTravelDist;
                            }
                        }

                        rayX += rayDeltaX;
                        rayY += rayDeltaY;
                        rayZ += rayDeltaZ;

                        rayTravelDist += 1.0f / Math.abs(delta);
                    }
                }

                int pixelR = (pixelColor >> 16 & 0xFF) * fogMultiplier / 0xFF;
                int pixelG = (pixelColor >> 8  & 0xFF) * fogMultiplier / 0xFF;
                int pixelB = (pixelColor       & 0xFF) * fogMultiplier / 0xFF;

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