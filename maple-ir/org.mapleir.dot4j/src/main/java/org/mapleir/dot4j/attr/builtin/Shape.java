package org.mapleir.dot4j.attr.builtin;

import static org.mapleir.dot4j.attr.Attrs.*;

import org.mapleir.dot4j.attr.Attr;
import org.mapleir.dot4j.attr.Attrs;

public class Shape extends Attr<String> {

    private static final String SHAPE = "shape";

    public static final Shape
    		BOX = new Shape("box"),
            ELLIPSE = new Shape("ellipse"),
            CIRCLE = new Shape("circle"),
            POINT = new Shape("point"),
            EGG = new Shape("egg"),
            TRIANGLE = new Shape("triangle"),
            DIAMOND = new Shape("diamond"),
            TRAPEZIUM = new Shape("trapezium"),
            PARALLELOGRAM = new Shape("parallelogram"),
            HOUSE = new Shape("house"),
            PENTAGON = new Shape("pentagon"),
            HEXAGON = new Shape("hexagon"),
            SEPTAGON = new Shape("septagon"),
            OCTAGON = new Shape("octagon"),
            DOUBLE_CIRCLE = new Shape("doublecircle"),
            DOUBLE_OCTAGON = new Shape("doubleoctagon"),
            TRIPLE_OCTAGON = new Shape("tripleoctagon"),
            INV_TRIANGLE = new Shape("invtriangle"),
            INV_TRAPEZIUM = new Shape("invtrapezium"),
            INV_HOUSE = new Shape("invhouse"),
            RECTANGLE = new Shape("rectangle"),
            NONE = new Shape("none");

    private Shape(String value) {
        super(SHAPE, value);
    }

    public static Attrs mDiamond(String topLabel, String bottomLabel) {
        return attrs(attr(SHAPE, "Mdiamond"), attr("toplabel", topLabel), attr("bottomlabel", bottomLabel));
    }

    public static Attrs mSquare(String topLabel, String bottomLabel) {
        return attrs(attr(SHAPE, "Msquare"), attr("toplabel", topLabel), attr("bottomlabel", bottomLabel));
    }

    public static Attrs mCircle(String topLabel, String bottomLabel) {
        return attrs(attr(SHAPE, "Mcircle"), attr("toplabel", topLabel), attr("bottomlabel", bottomLabel));
    }

    public static Attrs polygon(int sides, double skew, double distortion) {
        return attrs(attr(SHAPE, "polygon"), attr("sides", sides), attr("skew", skew), attr("distortion", distortion));
    }
}
