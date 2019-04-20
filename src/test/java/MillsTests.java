import org.junit.Test;

import java.util.Date;

public class MillsTests {

    /**
     * 距现在12个小时
     */
    @Test
    public void millSecondsTests(){
        Date now = new Date(System.currentTimeMillis());
        Date newNow = new Date(System.currentTimeMillis() - 43200000L);
        return;
    }
}
