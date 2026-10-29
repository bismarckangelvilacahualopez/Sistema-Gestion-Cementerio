import cementerio.Main;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Pruebas técnicas sin librerías externas. Se ejecutan en pantalla virtual si no hay escritorio. */
public class PruebaAutomatica {
    static int ok = 0;
    static void check(boolean b, String label) { if (!b) throw new AssertionError(label); System.out.println("OK: " + label); ok++; }
    static Object call(Object o, String name, Class<?>[] types, Object... args) throws Exception {
        Method m = o.getClass().getDeclaredMethod(name, types); m.setAccessible(true); return m.invoke(o, args);
    }
    static Object field(Object o, String name) throws Exception { Field f = o.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(o); }
    static void set(Object o, String name, Object value) throws Exception { Field f = o.getClass().getDeclaredField(name); f.setAccessible(true); f.set(o,value); }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        Path home = Files.createTempDirectory("cementerio-pruebas-");
        System.setProperty("user.home", home.toString());
        final Main[] app = new Main[1];
        SwingUtilities.invokeAndWait(() -> {
            try { Constructor<Main> c = Main.class.getDeclaredConstructor(); c.setAccessible(true); app[0] = c.newInstance(); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
        Object db = field(app[0], "db");
        Map<Integer,Object> pabellones = (Map<Integer,Object>)field(db,"pabellones");
        Map<Integer,Object> nichos = (Map<Integer,Object>)field(db,"nichos");
        Class<?> pab = Class.forName("cementerio.Main$Pabellon");
        Constructor<?> constructor = pab.getDeclaredConstructor(); constructor.setAccessible(true);
        Object p = constructor.newInstance(); set(p,"id",1); set(p,"nombre","Pabellón de prueba"); set(p,"descripcion","Prueba");
        pabellones.put(1,p);
        Method crear = Main.class.getDeclaredMethod("crearNichosCroquis",pab); crear.setAccessible(true);
        crear.invoke(app[0],p);
        check(nichos.size()==100,"Un pabellón genera exactamente 100 nichos");
        crear.invoke(app[0],p);
        check(nichos.size()==100,"No se duplican nichos al regenerar el croquis");
        long ocupados = nichos.values().stream().filter(n->{try{return (boolean)field(n,"ocupado");}catch(Exception e){throw new RuntimeException(e);}}).count();
        check(ocupados==0,"Los nichos nuevos están disponibles");
        Method precio = Main.class.getDeclaredMethod("precioFila",int.class); precio.setAccessible(true);
        check(((BigDecimal)precio.invoke(null,1)).intValue()==1000,"Tarifa fila inferior Bs 1000");
        check(((BigDecimal)precio.invoke(null,10)).intValue()==600,"Tarifa fila superior Bs 600");
        call(db,"save",new Class<?>[]{});
        check(Files.exists(home.resolve(".cementerio_municipal/datos.ser")),"Se guardan los datos locales");
        Method load = db.getClass().getDeclaredMethod("load"); load.setAccessible(true);
        Object restored = load.invoke(null);
        check(((Map<?,?>)field(restored,"nichos")).size()==100,"Se recuperan los 100 nichos guardados");
        SwingUtilities.invokeAndWait(()->app[0].dispose());
        System.out.println("PRUEBAS COMPLETADAS: " + ok + " de " + ok);
    }
}
