package skadistats;

import java.io.IOException;

import skadistats.spectre.*;
import skadistats.spectre.err.*;
import skadistats.spectre.proto.basic.Position.HeroPosition;

public class WritePosition {
    public static void main(String[] args) throws IOException, AspectNotFound {
        Spectre spectre = new Spectre("/tmp");

        AspectWriter writer = spectre.newWriter("/basic/position/hero", 1111);
        HeroPosition aPos = null;

        aPos = HeroPosition.newBuilder()
               .setHeroIdx(writer.indexString("AntiMage"))
               .setPosX(100)
               .setPosY(200)
               .build();
        writer.write(aPos);
        writer.close();

        AspectReader reader = spectre.newReader("/basic/position/hero", 1111);

        for (HeroPosition pos : reader.iterHeroPosition()) {
        }
    }
}
