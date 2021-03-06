package skadistats;

import java.io.IOException;
import java.util.Collection;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.*;
import ch.qos.logback.classic.encoder.*;
import ch.qos.logback.classic.spi.*;
import ch.qos.logback.core.*;

import skadistats.spectre.*;
import skadistats.spectre.err.*;
import skadistats.spectre.proto.basic.Position.EntityPosition;
import skadistats.spectre.proto.basic.Match.Draft;
import skadistats.spectre.proto.basic.Match.Draft.DraftEntry;
import skadistats.spectre.proto.basic.Player.TEAM;

public class WritePosition {
    public static void main(String[] args) throws IOException, AspectNotFound {
        Spectre spectre = new Spectre("skadistats-artifacts");

        //AspectWriter writer = spectre.newWriter("/basic/position/hero", 1111);
        //EntityPosition aPos = null;

        //writer.setTick(1);

        //aPos = EntityPosition.newBuilder()
        //       .setEntityIdx(writer.indexString("AntiMage"))
        //       .setPosX(100)
        //       .setPosY(200)
        //       .build();
        //writer.write(aPos);
        //writer.close();


        AspectReader reader = spectre.newReader("/basic/position/hero", 321957318);

        //System.out.println("Replay: "+reader.getReplayId());
        //for (Draft draft : reader.iterDraft()) {
        //    for (DraftEntry e : draft.getEntriesList()) {
        //        System.out.println(e.getOrder()+" : "+e.getSide()+" : "+e.getHeroIdx());
        //    }
        //}
        //for (EntityPosition pos : reader.iterEntityPosition()) {
        //    System.out.println(reader.getTick()+" : ["+pos.getPosX()+"/"+pos.getPosY()+"] "+reader.lookupString(pos.getEntityIdx()));
        //    break;
        //}

        for (Collection<EntityPosition> posBag : reader.iterEntityPositionByTick()) {
            System.out.println(reader.getTick()+" : "+posBag.size()+" messages");

            for (EntityPosition pos : posBag) {
                System.out.println("\t : ["+pos.getPosX()+"/"+pos.getPosY()+"] "+reader.lookupString(pos.getEntityIdx()));
            }
        }

        System.out.println("Done");
    }
}
