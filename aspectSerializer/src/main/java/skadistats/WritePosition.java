package skadistats;

import java.io.IOException;

import skadistats.spectre.AspectReader;
import skadistats.spectre.AspectWriter;
import skadistats.spectre.proto.basic.Position.HeroPosition;

public class WritePosition {
    public static void main(String[] args) throws IOException {
        String BASE = "/tmp";
        AspectWriter writer = AspectWriter.newWriter(BASE, "/basic/position/hero", 1111);
        HeroPosition aPos = null;

        aPos = HeroPosition.newBuilder()
               .setHeroIdx(writer.indexString("AntiMage"))
               .setPosX(100)
               .setPosY(200)
               .build();
        writer.write(aPos);

        AspectReader reader = AspectReader.newReader(BASE, "/basic/position/hero", 1111);

        for (HeroPosition pos : reader.iterHeroPosition()) {

        }
    }
}
