package modelo;

import java.util.ArrayList;

public class Blockchain {
    private ArrayList<Bloque> cadena;
    private int dificultad;
    private Bloque bloquePendiente; 

    public Blockchain(int dificultad) {
        this.dificultad = dificultad;
        this.cadena = new ArrayList<>();
        crearBloqueGenesis();
        // ⭐ Inicializar bloque pendiente
        this.bloquePendiente = new Bloque(obtenerUltimoBloque().getHash());
    }

    private void crearBloqueGenesis() {
        Bloque bloqueGenesis = new Bloque("0");
        bloqueGenesis.setIndex(0); 
        bloqueGenesis.minarBloque(dificultad);
        cadena.add(bloqueGenesis);
        System.out.println("✅ Bloque Génesis creado: " + bloqueGenesis.getHash());
    }
    
    public Bloque obtenerUltimoBloque() {
        return cadena.get(cadena.size() - 1);
    }

    public void agregarBloque(Bloque nuevoBloque) {
        // No minar dos veces
        nuevoBloque.setIndex(cadena.size());
        nuevoBloque.setHashAnterior(obtenerUltimoBloque().getHash());
        cadena.add(nuevoBloque);
        System.out.println("✅ Bloque #" + nuevoBloque.getIndex() + " agregado");
        System.out.println("📊 Total bloques: " + cadena.size());
    }

    public boolean esCadenaValida() {
        Bloque bloqueActual;
        Bloque bloqueAnterior;

        for (int i = 1; i < cadena.size(); i++) {
            bloqueActual = cadena.get(i);
            bloqueAnterior = cadena.get(i - 1);

            if (!bloqueActual.getHash().equals(bloqueActual.calcularHash())) {
                System.out.println("❌ Hash inválido en bloque #" + i);
                return false;
            }

            if (!bloqueActual.getHashAnterior().equals(bloqueAnterior.getHash())) {
                System.out.println("❌ Enlace roto en bloque #" + i);
                return false;
            }
        }
        return true;
    }

  
    public Bloque getBloquePendiente() {
        return bloquePendiente;
    }
    
    public void setBloquePendiente(Bloque bloque) {
        this.bloquePendiente = bloque;
    }
    
    public ArrayList<Bloque> getCadena() {
        return cadena;
    }
    
    public void setCadena(ArrayList<Bloque> nuevaCadena) {
    this.cadena = nuevaCadena;
    System.out.println("🔄 Cadena reemplazada. Total bloques: " + cadena.size());
}

    public int getDificultad() {
        return dificultad;
    }
}