import junit.framework.TestCase;

/**
 * @author Alexey Donov
 */
public class ReplaceTest extends TestCase {
    public void testReplace() {
        final String origin = "obs!2017_08_15_15_25_36-N-DD_HH_II_SS-DD_HH_II_SS.mp4";

        final String newFileName = origin
            .replaceAll("N", "" + 1)
            .replaceFirst("DD", "" + 10)
            .replaceFirst("HH", "" + 20)
            .replaceFirst("II", "" + 30)
            .replaceFirst("SS", "" + 40)
            .replaceFirst("DD", "" + 50)
            .replaceFirst("HH", "" + 60)
            .replaceFirst("II", "" + 70)
            .replaceFirst("SS", "" + 80);

        System.out.println(newFileName);

        assertEquals("obs!2017_08_15_15_25_36-1-10_20_30_40-50_60_70_80.mp4", newFileName);
    }
}
