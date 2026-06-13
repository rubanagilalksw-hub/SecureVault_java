package securevault;

import java.awt.*;

public class WrapLayout extends FlowLayout {
    public WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }

    @Override public Dimension preferredLayoutSize(Container target) { return layoutSize(target, true); }
    @Override public Dimension minimumLayoutSize(Container target) { Dimension d = layoutSize(target,false); d.width-=(getHgap()+1); return d; }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int targetWidth = target.getSize().width;
            Container c = target;
            while (c.getSize().width == 0 && c.getParent() != null) c = c.getParent();
            targetWidth = c.getSize().width;
            if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;
            int hgap = getHgap(), vgap = getVgap();
            Insets ins = target.getInsets();
            int maxW = targetWidth - ins.left - ins.right - hgap*2;
            Dimension dim = new Dimension(0,0);
            int rowW = 0, rowH = 0;
            for (int i = 0; i < target.getComponentCount(); i++) {
                Component m = target.getComponent(i);
                if (m.isVisible()) {
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                    if (rowW + d.width > maxW) { addRow(dim, rowW, rowH); rowW = 0; rowH = 0; }
                    if (rowW != 0) rowW += hgap;
                    rowW += d.width; rowH = Math.max(rowH, d.height);
                }
            }
            addRow(dim, rowW, rowH);
            dim.width += ins.left + ins.right + hgap*2;
            dim.height += ins.top + ins.bottom + vgap*2;
            return dim;
        }
    }
    private void addRow(Dimension dim, int rowW, int rowH) { dim.width = Math.max(dim.width, rowW); if (dim.height > 0) dim.height += getVgap(); dim.height += rowH; }
}
