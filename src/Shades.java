import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Shades {
    // Mapa clave valor donde la clave es el X donde se evaluo
    // El valor es la imagen valuada en (f_x, g_x)
    private final Map<Integer, Pair> evaluatedValues = new HashMap<>();
    private final static GF251 GF251 = new GF251();

    // Constructor para distribucion
    public Shades(Block block, int n) {
        for (int i = 1; i <= n; i++) {
            evaluatedValues.put(i, new Pair(block.getF().evaluate(i), block.getG().evaluate(i)));
        }
    }

    // Constructor para recuperación
    public Shades(List<Integer> x, List<Integer> f_x, List<Integer> g_x) {
        for (int i = 0; i < x.size(); i++) {
            evaluatedValues.put(x.get(i), new Pair(f_x.get(i), g_x.get(i)));
        }
    }

    public List<Polynomial> applyLagrange(int k) {
        if (evaluatedValues.size() < k) {
            System.out.println("No es posible hallar la imagen con menos de k sombras");
            System.exit(1);
        }

        List<Polynomial> interpolatedPolynomials = new ArrayList<>();

        List<Pair> recoveredF = new ArrayList<>();
        List<Pair> recoveredG = new ArrayList<>();

        // Convertimos entries del map a Pair(x,f_x) y Pair(x,g_x)
        for (Map.Entry<Integer, Pair> entry : evaluatedValues.entrySet()) {
            recoveredF.add(new Pair(entry.getKey(), entry.getValue().getLeft()));
            recoveredG.add(new Pair(entry.getKey(), entry.getValue().getRight()));
        }

        interpolatedPolynomials.add(lagrangeInterpolation(recoveredF, k));
        interpolatedPolynomials.add(lagrangeInterpolation(recoveredG, k));

        if (!detectCheating(interpolatedPolynomials.get(0), interpolatedPolynomials.get(1))) {
            System.out.println("Cheating detectado");
            System.exit(1);
        }

        return interpolatedPolynomials;
    }

    private Polynomial lagrangeInterpolation(List<Pair> shades, int k) {
        List<Integer> resultSi = new ArrayList<>();
        Integer result;
        List<Pair> shadesAux = new ArrayList<>(shades.subList(0, k));
        List<Pair> remainingShades = new ArrayList<>(shades.subList(k, shades.size()));

        // Se usan las primeras k sombras para interpolar el polinomio
        while (shadesAux.size() > 0) {
            result = 0;
            for (int i = 0; i < shadesAux.size(); i++) {
                result += (calculateLi(shadesAux.get(i), shadesAux) * shadesAux.get(i).getRight());
            }
            result = GF251.transformToGF(result);
            resultSi.add(result);
            shadesAux.remove(shadesAux.size() - 1);
            recalculateY(shadesAux, result);
        }

        Polynomial toReturn = new Polynomial(resultSi);

        // Se verifica que el polinomio interpolado cumpla con las demás sombras
        // Si alguna no valida, entonces hubo cheating fuera de la interpolación o el k seleccionado no es válido
        for (Pair shade : remainingShades) {
            if (!toReturn.evaluate(shade.getLeft()).equals(shade.getRight())) {
                System.out.println("Fallo en la interpolación. Valor de k inválido o cheating");
                System.exit(1);
            }
        }

        return toReturn;
    }

    private void recalculateY(List<Pair> shades, int si) {
        for (Pair shade : shades) {
            Integer aux = ((shade.getRight() - si) * GF251.getInverse(shade.getLeft()));
            aux = GF251.transformToGF(aux);
            shade.setRight(aux);
        }
    }

    private int calculateLi(Pair currentShade, List<Pair> recoveredShades) {
        int result = 1;
        for (Pair recoveredShade : recoveredShades) {
            if (!recoveredShade.equals(currentShade)) {
                Integer denominator = ((currentShade.getLeft() - recoveredShade.getLeft()));
                denominator = GF251.transformToGF(denominator);
                result *= -recoveredShade.getLeft() * GF251.getInverse(denominator);
                result = GF251.transformToGF(result);
            }
        }
        return result;
    }

    private boolean detectCheating(Polynomial f, Polynomial g) {
        int ri_1 = GF251.transformToGF(-g.getCoefficient(0) * GF251.getInverse(f.getCoefficient(0)));
        int ri_2 = GF251.transformToGF(-g.getCoefficient(1) * GF251.getInverse(f.getCoefficient(1)));
        return ri_1 == ri_2;
    }

    public Pair getPair(Integer n) {
        return evaluatedValues.get(n);
    }

    protected static class Pair {
        private Integer left;
        private Integer right;

        public Pair(Integer left, Integer right) {
            this.left = left;
            this.right = right;
        }

        public Integer getLeft() {
            return left;
        }

        public Integer getRight() {
            return right;
        }

        public void setLeft(Integer left) {
            this.left = left;
        }

        public void setRight(Integer right) {
            this.right = right;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof Pair)) {
                return false;
            }
            Pair other = (Pair) obj;
            return this.left.equals(other.left) && this.right.equals(other.right);
        }
    }
}
