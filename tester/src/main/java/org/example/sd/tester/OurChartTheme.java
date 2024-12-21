/*
 * Copyright 2024 Carolina Pereira, Diogo Costa, Humberto Gomes, Sara Lopes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.example.sd.tester;

import java.awt.Color;
import java.awt.Font;

import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.ui.RectangleInsets;

public class OurChartTheme extends StandardChartTheme {
    public OurChartTheme() {
        super("Our custom theme");

        // Use LaTeX fonts
        Font oldExtraLargeFont = this.getExtraLargeFont();
        Font oldLargeFont      = this.getLargeFont();
        Font oldRegularFont    = this.getRegularFont();
        Font oldSmallFont      = this.getSmallFont();

        Font extraLargeFont =
            new Font("CMU Serif", oldExtraLargeFont.getStyle(), oldExtraLargeFont.getSize());
        Font largeFont = new Font("CMU Serif", oldLargeFont.getStyle(), oldLargeFont.getSize());
        Font regularFont =
            new Font("CMU Typewriter Text", oldRegularFont.getStyle(), oldRegularFont.getSize());
        Font smallFont =
            new Font("CMU Typewriter Text", oldSmallFont.getStyle(), oldSmallFont.getSize());

        this.setExtraLargeFont(extraLargeFont);
        this.setLargeFont(largeFont);
        this.setRegularFont(regularFont);
        this.setSmallFont(smallFont);

        // Adjust colors
        this.setBarPainter(new StandardBarPainter());
        this.setRangeGridlinePaint(Color.decode("#C0C0C0"));
        this.setPlotBackgroundPaint(Color.white);
        this.setChartBackgroundPaint(Color.white);
        this.setAxisOffset(new RectangleInsets(0, 0, 0, 0));
    }
}
