package fr.cnrs.liris.jpugetgil.converg.utils;

public class Pair<L, R> {
    private L left;
    private R right;

    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    public L getLeft() {
        return left;
    }

    public R getRight() {
        return right;
    }

    public void setLeft(L left) {
        this.left = left;
    }

    public void setRight(R right) {
        this.right = right;
    }

    @Override
    public int hashCode() {
        return left.hashCode() ^ right.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Pair<?, ?>)) return false;
        Pair<?, ?> pairo = (Pair<?, ?>) o;
        return (left == null ? pairo.left == null : left.equals(pairo.left)) &&
               (right == null ? pairo.right == null : right.equals(pairo.right));
    }
}