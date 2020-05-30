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
    
    final static int MOUSE_X = 2;
    final static int MOUSE_Y = 3;
    
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

    BufferedImage screen = new BufferedImage(214, 120, 1);
    
    public void run() {
        try {
            Random rand = new Random(18295169L);
            int[] screenBuffer = ((DataBufferInt)screen.getRaster().getDataBuffer()).getData();
            
            int[] worldArray = new int[262144]; // 2^18
            
            // fill world with random blocks
            for (int i = 0; i < 262144; i++) {
                int block;
                if(i / 64 % 64 > 32 + rand.nextInt(8))
                    block = (rand.nextInt(8) + 1);
                else
                    block = 0;
                
                worldArray[i] = block;
            }
            
            int[] arrayOfInt3 = new int[12288];
            for (int j = 1; j < 16; j++) {
                int k = 255 - rand.nextInt(96);
                
                for (int m = 0; m < 48; m++) {
                    for (int n = 0; n < 16; n++) {
                        int i1 = 9858122;
                        if (j == 4)
                            i1 = 8355711;
                        
                        if (j != 4 || rand.nextInt(3) == 0)
                            k = 255 - rand.nextInt(96);
                        
                        if (j == 1 && m < (n * n * 3 + n * 81 >> 2 & 0x3) + 18)
                            i1 = 6990400;
                        else if (j == 1 && m < (n * n * 3 + n * 81 >> 2 & 0x3) + 19)
                            k = k * 2 / 3;
                            
                        if (j == 7) {
                            i1 = 6771249;
                            if (n > 0 && n < 15 && ((m > 0 && m < 15) || (m > 32 && m < 47))) {
                                i1 = 12359778;
                                int i2 = n - 7;
                                int i3 = (m & 0xF) - 7;
                                
                                if (i2 < 0)
                                    i2 = 1 - i2;
                                
                                if (i3 < 0)
                                    i3 = 1 - i3;
                                
                                if (i3 > i2)
                                    i2 = i3;
                                
                                k = 196 - rand.nextInt(32) + i2 % 3 * 32;
                            } else if (rand.nextInt(2) == 0) {
                                k = k * (150 - (n & 1) * 100) / 100;
                            }
                        }
                        
                        if (j == 5) {
                            i1 = 11876885;
                            if ((n + m / 4 * 4) % 8 == 0 || m % 4 == 0)
                                i1 = 12365733;
                        }
                        
                        int i2 = k;
                        if (m >= 32)
                            i2 /= 2;
                        
                        if (j == 8) {
                            i1 = 5298487;
                            if (rand.nextInt(2) == 0) {
                                i1 = 0;
                                i2 = 255;
                            }
                        }
                        int i3 = (i1 >> 16 & 0xFF) * i2 / 255 << 16 | (i1 >> 8 & 0xFF) * i2 / 255 << 8 | (i1 & 0xFF) * i2 / 255;
                        arrayOfInt3[n + m * 16 + j * 256 * 3] = i3;
                    }
                }
            }
            
            float f1 = 96.5F;
            float f2 = 65.0F;
            float f3 = 96.5F;
            float f4 = 0.0F;
            float f5 = 0.0F;
            float f6 = 0.0F;
            long startTime = System.currentTimeMillis();
            int i4 = -1;
            int i5 = 0;
            float f7 = 0.0F;
            float f8 = 0.0F;
            
            while (true) {
                float f9 = (float)Math.sin(f7);
                float f10 = (float)Math.cos(f7);
                float f11 = (float)Math.sin(f8);
                float f12 = (float)Math.cos(f8);
                
                while (System.currentTimeMillis() - startTime > 10L) {
                    if (input[MOUSE_X] > 0) {
                        float f13 = (input[MOUSE_X] - 428) / 214.0F * 2.0F;
                        float f14 = (input[MOUSE_Y] - 240) / 120.0F * 2.0F;
                        float f15 = (float)Math.sqrt((f13 * f13 + f14 * f14)) - 1.2F;
                        
                        if (f15 < 0.0F)
                            f15 = 0.0F;
                        
                        if (f15 > 0.0F) {
                            f7 += f13 * f15 / 400.0F;
                            f8 -= f14 * f15 / 400.0F;
                            
                            if (f8 < -1.57F)
                                f8 = -1.57F;
                            
                            if (f8 > 1.57F)
                                f8 = 1.57F;
                        }
                    }
                    
                    startTime += 10L;
                    float inputX = (input[KeyEvent.VK_D] - input[KeyEvent.VK_A]) * 0.02F;
                    float inputZ = (input[KeyEvent.VK_W] - input[KeyEvent.VK_S]) * 0.02F;
                    f4 *= 0.5F;
                    f5 *= 0.99F;
                    f6 *= 0.5F;
                    f4 += f9 * inputZ + f10 * inputX;
                    f6 += f10 * inputZ - f9 * inputX;
                    f5 += 0.003F;
                    int i8;
                    
                    OUTER:
                    for (i8 = 0; i8 < 3; i8++) {
                        float f16 = f1 + f4 * ((i8 + 0) % 3 / 2);
                        float f17 = f2 + f5 * ((i8 + 1) % 3 / 2);
                        float f19 = f3 + f6 * ((i8 + 2) % 3 / 2);
                        
                        for (int i12 = 0; i12 < 12; i12++) {
                            int i13 = (int)(f16 + (i12 >> 0 & 1) * 0.6F - 0.3F) - 64;
                            int i14 = (int)(f17 + ((i12 >> 2) - 1) * 0.8F + 0.65F) - 64;
                            int i15 = (int)(f19 + (i12 >> 1 & 1) * 0.6F - 0.3F) - 64;
                            
                            if (i13 < 0 || i14 < 0 || i15 < 0 || i13 >= 64 || i14 >= 64 || i15 >= 64 || worldArray[i13 + i14 * 64 + i15 * 4096] > 0) {
                                if (i8 != 1)
                                    break OUTER;
                                
                                if (input[KeyEvent.VK_SPACE] > 0 && f5 > 0.0F) {
                                    input[KeyEvent.VK_SPACE] = 0;
                                    f5 = -0.1F;
                                    break OUTER;
                                }
                                
                                f5 = 0.0F;
                                break OUTER;
                            }
                        }
                        
                        f1 = f16;
                        f2 = f17;
                        f3 = f19;
                    }
                }
                
                int i6 = 0;
                int i7 = 0;
                
                if (input[1] > 0 && i4 > 0) {
                    worldArray[i4] = 0;
                    input[1] = 0;
                }
                
                if (input[0] > 0 && i4 > 0) {
                    worldArray[i4 + i5] = 1;
                    input[0] = 0;
                }
                
                for (int i8 = 0; i8 < 12; i8++) {
                    int i9 = (int)(f1 + (i8 >> 0 & 1) * 0.6F - 0.3F) - 64;
                    int i10 = (int)(f2 + ((i8 >> 2) - 1) * 0.8F + 0.65F) - 64;
                    int i11 = (int)(f3 + (i8 >> 1 & 1) * 0.6F - 0.3F) - 64;
                    
                    if (i9 >= 0 && i10 >= 0 && i11 >= 0 && i9 < 64 && i10 < 64 && i11 < 64)
                        worldArray[i9 + i10 * 64 + i11 * 4096] = 0;
                }
                
                // render the screen
                float i8 = -1.0F;
                for (int i9 = 0; i9 < 214; i9++) {
                    float f18 = (i9 - 107) / 90.0F;
                    
                    for (int i11 = 0; i11 < 120; i11++) {
                        float f20 = (i11 - 60) / 90.0F;
                        float f21 = 1.0F;
                        float f22 = f21 * f12 + f20 * f11;
                        float f23 = f20 * f12 - f21 * f11;
                        float f24 = f18 * f10 + f22 * f9;
                        float f25 = f22 * f10 - f18 * f9;
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
                            float f32 = f1 - (int)f1;
                            
                            if (i18 == 1)
                                f32 = f2 - (int)f2;
                            
                            if (i18 == 2)
                                f32 = f3 - (int)f3;
                            
                            if (f27 > 0.0F)
                                f32 = 1.0F - f32;
                            
                            float f33 = f28 * f32;
                            float f34 = f1 + f29 * f32;
                            float f35 = f2 + f30 * f32;
                            float f36 = f3 + f31 * f32;
                            
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
                                int i25 = worldArray[i24];
                                
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
                                    if (i24 != i4 || (i6 > 0 && i7 % 16 > 0 && i6 < 15 && i7 % 16 < 15))
                                        i26 = arrayOfInt3[i6 + i7 * 16 + i25 * 256 * 3];
                                    
                                    if (f33 < f26 && i9 == input[MOUSE_X] / 4 && i11 == input[MOUSE_Y] / 4) {
                                        i8 = i24;
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
                        screenBuffer[i9 + i11 * 214] = i18 << 16 | i19 << 8 | i20;
                    }
                }
                
                i4 = (int)i8;
                
                Thread.sleep(20L);
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
        System.out.println(e.getKeyCode());
        
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
            Minecraft4k.input[1] = 1;
            return;
        }
        Minecraft4k.input[0] = 1;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isMetaDown()) {
            Minecraft4k.input[1] = 0;
            return;
        }
        Minecraft4k.input[0] = 0;
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