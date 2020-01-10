package examplefuncsplayer;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

public class RobotPlayerTest {

    @Test
    public void testSanity() {
        assertEquals(2, 1+1);
    }

    @Test
    public void testEncryptDecrypt() {
        Random rnd = new Random();
        for(int i = 0; i < 100; ++i){
            int[] msg = new int[7];
            for(int j = 0; j < 7; ++j)
                msg[j] = rnd.nextInt();
            assertArrayEquals(msg, RobotPlayer.Crypto.Decrypt(RobotPlayer.Crypto.Encrypt(msg)));
            assertArrayEquals(msg, RobotPlayer.Crypto.Encrypt(RobotPlayer.Crypto.Decrypt(msg)));
        }
    }

}
