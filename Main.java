import java.util.PriorityQueue;
import javax.swing.Timer;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;

public class Main {
    static int maxMemoria = 25;
    static int memoriaOcupada = 0;
    static int noProcesos = 1;
    static int procesosEnCola = 0;
    public static void main(String args[]) {
        HiloGenerador generador = new HiloGenerador();
        HiloSO sistemaOperativo = new HiloSO(generador.getColaPrioridad());
        
        generador.start();
        sistemaOperativo.start();

        Vista vista = new Vista(generador);
    }

    //Clases Hilos

    static class HiloProceso extends Thread {
        private String nombre;
        private int tiempo;
        private int tiempoRecibido;
        private int prioridad;
        private int memoriaNecesaria;
        private int estaEnRam;
        private PriorityQueue<HiloProceso> cola;

        public HiloProceso(String nombre, int tiempo, int prioridad, int memoriaNecesaria, PriorityQueue<HiloProceso> cola) {
            this.nombre = nombre;
            this.tiempo = tiempo;
            this.tiempoRecibido = 0;
            this.prioridad = prioridad;
            this.memoriaNecesaria = memoriaNecesaria;
            this.estaEnRam = 0;
            this.cola = cola;
        }

        public void recibirTiempo() {
            System.out.println("Soy el HiloProceso: " + nombre + " y acabo de entrar.");
            while (tiempo > tiempoRecibido) {
                try {
                    Thread.sleep(1000);
                    tiempoRecibido++;
                } catch (InterruptedException e) {
                    System.err.println("Error en los hilos proceso");
                }
            }
            memoriaOcupada -= memoriaNecesaria; // Liberar memoria cuando el proceso termina
            System.out.println("Soy el HiloProceso: " + nombre + " y ya termine.");
            if (procesosEnCola > 0){
                procesosEnCola--;
            }
            cola.remove(this);
        }

        public String getNombre(){
            return nombre;
        }

        public int getTiempo(){
            return tiempo;
        }

        public int getTiempoRecibido(){
            return tiempoRecibido;
        }

        public int getPrioridad(){
            return prioridad;
        }

        public int getMemoriaNecesaria(){
            return memoriaNecesaria;
        }

        public void setEstaEnRam(int estaEnRam){
            this.estaEnRam = estaEnRam;
        }
    }

    static class HiloSO extends Thread {
        private PriorityQueue<HiloProceso> colaPrioridad;

        public HiloSO(PriorityQueue<HiloProceso> colaPrioridad) {
            this.colaPrioridad = colaPrioridad;
        }

        public HiloProceso escogerHilo() {
            return colaPrioridad.poll();
        }

        @Override
        public void run() {
            while (true) {
                if (!colaPrioridad.isEmpty()) {
                    HiloProceso aux = escogerHilo();
                    aux.start();
                    aux.recibirTiempo();
                    try {
                        Thread.sleep(aux.tiempo * 1000);
                    } catch (InterruptedException e) {
                        System.err.println("Error al dormir al hilo SO");
                    }
                }
            }
        }
    }

    static class HiloGenerador extends Thread {
        private PriorityQueue<HiloProceso> colaPrioridad;

        public HiloGenerador() {
            colaPrioridad = new PriorityQueue<>((p1, p2) -> p2.getPrioridad() - p1.getPrioridad());
        }

        public PriorityQueue<HiloProceso> getColaPrioridad() {
            return colaPrioridad;
        }

        public void generarProceso(String nombre, int prioridad, int tiempo, int memoriaNecesaria) {
            if (memoriaOcupada + memoriaNecesaria <= maxMemoria) {
                HiloProceso proceso = new HiloProceso(nombre, tiempo, prioridad, memoriaNecesaria, colaPrioridad);
                colaPrioridad.add(proceso);
                memoriaOcupada += memoriaNecesaria;
            } else if (procesosEnCola < 5) {
                HiloProceso proceso = new HiloProceso(nombre, tiempo, prioridad, memoriaNecesaria, colaPrioridad);
                colaPrioridad.add(proceso);
                procesosEnCola++;
            }else{
                System.out.println("Memoria llena o limite de hilos adicionales alcanzado. No se puede agregar mas procesos en este momento.");
            }
        }

        public void eliminarProceso(HiloProceso proceso) {
            colaPrioridad.remove(proceso);
        }        

        @Override
        public void run() {
            while (true) {
                int prioridad = (int) (Math.random() * 3) + 1; // Prioridad aleatoria
                int tiempo = (int) (Math.random() * 3) + 1; // Tiempo aleatorio
                int memoria = (int) (Math.random() * 5) + 1; // Memoria aleatoria
                String nombre = "Proceso" + noProcesos;
                noProcesos++;
                generarProceso(nombre, prioridad, tiempo, memoria);
                try {
                    Thread.sleep(1000); // Espera 1 segundos antes de generar otro proceso
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //Clases GUI

    static class Vista extends JFrame{
        private Ram hilos;
        private Actual actual;
        private Memoria memoria;
        private ColaEspera espera;
        private HiloGenerador generador;

        public Vista (HiloGenerador generador){
            this.generador = generador;

            hilos = new Ram();
            actual = new Actual();
            memoria = new Memoria();
            espera = new ColaEspera();

            setLayout(new BorderLayout());
            setSize(500, 500);

            setLocationRelativeTo(null);

            add(hilos, BorderLayout.NORTH);
            add(actual, BorderLayout.CENTER);
            add(memoria, BorderLayout.EAST);
            add(espera, BorderLayout.SOUTH);

            setVisible(true);

            Timer timer = new Timer(500, e -> actualizarInterfaz());
    
            timer.start();
        }

        //Actualizar Interfaz

        private void actualizarInterfaz() {
            remove(hilos);
            remove(actual);
            remove(memoria);
            remove(espera);
            hilos = new Ram();
            actual = new Actual();
            memoria = new Memoria();
            espera = new ColaEspera();
            add(hilos, BorderLayout.NORTH);
            add(actual, BorderLayout.CENTER);
            add(memoria, BorderLayout.EAST);
            add(espera, BorderLayout.SOUTH);
            revalidate();
            repaint();
        }

        //Componente GUI

        public class Ram extends JPanel{
            public Ram(){
                setLayout(new BorderLayout());

                JLabel titulo = new JLabel();
                titulo.setText("Memoria Ram");

                JPanel procesosPanel = new JPanel();
                procesosPanel.setLayout(new GridLayout(6, 1));

                int memoriaUsada = 0;
                int procesosMostrados = 0;
                boolean excesoProcesos = false;

                for (HiloProceso proceso : generador.getColaPrioridad()) {
                    if (memoriaUsada + proceso.getMemoriaNecesaria() <= Main.maxMemoria && procesosMostrados < 5) {
                        JLabel procesoLabel = new JLabel();
                        procesoLabel.setText("Proceso: " + proceso.nombre + ", Prioridad: " + proceso.getPrioridad() + ", Memoria: " + proceso.getMemoriaNecesaria());
                        procesosPanel.add(procesoLabel);
        
                        memoriaUsada += proceso.getMemoriaNecesaria();
                        proceso.setEstaEnRam(1);
                        procesosMostrados++;
                    } else if (memoriaUsada + proceso.getMemoriaNecesaria() <= Main.maxMemoria && procesosMostrados == 5) {
                        excesoProcesos = true;
                        break; // Se ha alcanzado el límite de procesos o memoria disponible
                    }
                }
        
                if (excesoProcesos) {
                    JLabel extraProcesos = new JLabel();
                    extraProcesos.setText("+");
                    procesosPanel.add(extraProcesos);
                }

                add(titulo, BorderLayout.NORTH);
                add(procesosPanel, BorderLayout.CENTER);
            }
        }

        public class Actual extends JPanel{
            public Actual(){
                setLayout(new GridLayout(2, 1));

                JLabel titulo = new JLabel();
                titulo.setText("Proceso\nActual");
                add(titulo);

                JLabel procesoAtendido = new JLabel();
                HiloProceso procesoActual = generador.getColaPrioridad().peek();
                
                if (procesoActual != null){
                    procesoAtendido.setText("Proceso: " + procesoActual.nombre + ", Prioridad: " + procesoActual.getPrioridad() + 
                    ", Memoria: " + procesoActual.getMemoriaNecesaria());
                    add(procesoAtendido);
                }
            }
        }

        public class Memoria extends JPanel{
            public Memoria(){
                setLayout(new GridLayout(2, 2));

                JLabel memoriaTotal = new JLabel();
                memoriaTotal.setText("Memoria Total: ");

                JLabel noMemoriaTotal = new JLabel();
                noMemoriaTotal.setText(String.valueOf(maxMemoria));

                JLabel memoriaRestante = new JLabel();
                memoriaRestante.setText("Memoria Restante: ");

                JLabel noMemoriaRestante = new JLabel();
                noMemoriaRestante.setText(String.valueOf(maxMemoria - memoriaOcupada));

                add(memoriaTotal);
                add(noMemoriaTotal);
                add(memoriaRestante);
                add(noMemoriaRestante);
            }
        }

        public class ColaEspera extends JPanel{
            public ColaEspera() {
                setLayout(new BorderLayout());

                JLabel titulo = new JLabel("Procesos en espera");
                add(titulo, BorderLayout.NORTH);

                JPanel procesosPanel = new JPanel(new GridLayout(6, 1));

                int procesosEncolados = 0;

                for (HiloProceso proceso : generador.getColaPrioridad()){
                    if (proceso.estaEnRam == 0 && procesosEncolados < 5){
                        JLabel procesoLabel = new JLabel();
                        procesoLabel.setText("Proceso: " + proceso.nombre + ", Prioridad: " + proceso.getPrioridad() + ", Memoria: " + proceso.getMemoriaNecesaria());
                        procesosPanel.add(procesoLabel);
        
                        procesosEncolados++;
                    }else if (proceso.estaEnRam == 0 && procesosEncolados == 5){
                        JLabel extraProcesos = new JLabel();
                        extraProcesos.setText("+");
                        procesosPanel.add(extraProcesos);
                        procesosEncolados++;
                    }else if (procesosEncolados > 5){
                        break;
                    }
                }

                add(procesosPanel, BorderLayout.CENTER);
            }
        }
    }
}
