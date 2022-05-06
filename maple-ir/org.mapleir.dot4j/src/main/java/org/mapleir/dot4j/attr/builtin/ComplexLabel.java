package org.mapleir.dot4j.attr.builtin;

import org.mapleir.dot4j.attr.Attrs;
import org.mapleir.dot4j.attr.MapAttrs;

public class ComplexLabel extends Label implements Attrs {

    public enum Justification {
        LEFT, MIDDLE, RIGHT
    }

    public enum Location {
        TOP, CENTER, BOTTOM
    }
    
    private final boolean external;
    private final boolean floating;
    private final boolean decorated;
    private final Justification just;
    private final Location loc;

    private ComplexLabel(String value, boolean html, boolean external, boolean floating, boolean decorated,
                  Justification just, Location loc) {
        super(value, html);
        this.external = external;
        this.floating = floating;
        this.decorated = decorated;
        this.just = just;
        this.loc = loc;
    }
    
    public EndLabel head() {
        return EndLabel.head(this, null, null);
    }

    public EndLabel head(double angle, double distance) {
        return EndLabel.head(this, angle, distance);
    }

    public EndLabel tail() {
        return EndLabel.tail(this, null, null);
    }

    public EndLabel tail(double angle, double distance) {
        return EndLabel.tail(this, angle, distance);
    }

    public ComplexLabel external() {
        return new ComplexLabel(value, html, true, floating, decorated, just, loc);
    }

    public ComplexLabel floating() {
        return new ComplexLabel(value, html, external, true, decorated, just, loc);
    }

    public ComplexLabel decorated() {
        return new ComplexLabel(value, html, external, floating, true, just, loc);
    }

    public ComplexLabel justify(Justification just) {
        return new ComplexLabel(value, html, external, floating, decorated, just, loc);
    }

    public ComplexLabel locate(Location loc) {
        return new ComplexLabel(value, html, external, floating, decorated, just, loc);
    }
    
    public boolean isExternal() {
    	return external;
    }

	@Override
	public Attrs applyTo(MapAttrs mapAttrs) {
        mapAttrs.put(external ? "xlabel" : "label", this);
        if (floating) {
            mapAttrs.put("labelfloat", true);
        }
        if (decorated) {
            mapAttrs.put("decorate", true);
        }
        if (just == Justification.LEFT) {
            mapAttrs.put("labeljust", "l");
        }
        if (just == Justification.RIGHT) {
            mapAttrs.put("labeljust", "r");
        }
        if (loc == Location.TOP) {
            mapAttrs.put("labelloc", "t");
        }
        if (loc == Location.BOTTOM) {
            mapAttrs.put("labelloc", "b");
        }
        return mapAttrs;
	}
	
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Label label = (Label) o;

        return !(value != null ? !value.equals(label.value) : label.value != null);
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }

    @Override
    public String toString() {
        return value;
    }
    
	public static ComplexLabel of(String value) {
		return new ComplexLabel(value, false, false, false, false, null, null);
	}
	
	public static ComplexLabel html(String value) {
		return new ComplexLabel(value, true, false, false, false, null, null);
	}
}
