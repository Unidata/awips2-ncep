/**
 * 
 * gov.noaa.nws.ncep.ui.nsharp.natives.NsharpDataHandling
 * 
 * This java class performs the NSHARP NsharpDataHandling functions.
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    	Engineer    Description
 * -------		------- 	-------- 	-----------
 * 03/23/2010	229			Chin Chen	Initial coding
 * 10/20/2015   R12599      Chin Chen   negative PWAT value when surface layer dew point is missing 
 * 07/05/2016   RM#15923    Chin Chen   NSHARP - Native Code replacement
 * 
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 */

package gov.noaa.nws.ncep.ui.nsharp.natives;

import gov.noaa.nws.ncep.edex.common.nsharpLib.NsharpLibBasics;
import gov.noaa.nws.ncep.edex.common.sounding.NcSoundingLayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NsharpDataHandling {

    // compare two layers based on reverse height, pressure, then wind
    public static Comparator<NcSoundingLayer> reverseHeightPressureWindComparator() {

        return new Comparator<NcSoundingLayer>() {

            @Override
            public int compare(NcSoundingLayer layerA, NcSoundingLayer layerB) {
                int retValue = 0;
                if (layerA != layerB) {
                    // reverse sort relative to height!
                    retValue = Double.compare(layerB.getGeoHeight(),
                            layerA.getGeoHeight());

                    if (retValue == 0) {
                        // the two layers have same height. sort them by
                        // pressure
                        retValue = Double.compare(layerA.getPressure(),
                                layerB.getPressure());

                        if (retValue == 0) {
                            // the two layers have same height. sort them by
                            // wind
                            retValue = Double.compare(layerB.getWindSpeed(),
                                    layerA.getWindSpeed());
                        }
                    }
                }
                return retValue;
            }
        };
    }

    // compare two layers based on reverse pressure, height, then wind
    public static Comparator<NcSoundingLayer> reversePressureHeightWindComparator() {

        return new Comparator<NcSoundingLayer>() {

            @Override
            public int compare(NcSoundingLayer layerA, NcSoundingLayer layerB) {
                int retValue = 0;
                if (layerA != layerB) {
                    // reverse sort relative to pressure!
                    retValue = Double.compare(layerB.getPressure(),
                            layerA.getPressure());

                    if (retValue == 0) {
                        // the two layers have same height. sort them by height
                        retValue = Double.compare(layerA.getGeoHeight(),
                                layerB.getGeoHeight());

                        if (retValue == 0) {
                            // the two layers have same height. sort them by
                            // wind
                            retValue = Double.compare(layerB.getWindSpeed(),
                                    layerA.getWindSpeed());
                        }
                    }
                }
                return retValue;
            }
        };
    }

    // compare two layers based on reverse pressure,
    public static Comparator<NcSoundingLayer> reversePressureComparator() {

        return new Comparator<NcSoundingLayer>() {

            @Override
            public int compare(NcSoundingLayer layerA, NcSoundingLayer layerB) {
                int retValue = 0;
                if (layerA != layerB) {
                    // reverse sort relative to pressure!
                    retValue = Double.compare(layerB.getPressure(),
                            layerA.getPressure());
                }
                return retValue;
            }
        };
    }

    private static List<NcSoundingLayer> replaceNanSoundingData(
            List<NcSoundingLayer> soundingLys) {
        for (NcSoundingLayer layer : soundingLys) {
            if (Float.isNaN(layer.getGeoHeight()) == true) {
                layer.setGeoHeight(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
            }
            if (Float.isNaN(layer.getPressure()) == true) {
                layer.setPressure(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
            }
            if (Float.isNaN(layer.getTemperature()) == true) {
                layer.setTemperature(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
            }
            if (Float.isNaN(layer.getDewpoint()) == true) {
                layer.setDewpoint(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
            }
            if (Float.isNaN(layer.getWindDirection()) == true) {
                layer.setWindDirection(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
            }
            if (Float.isNaN(layer.getWindSpeed()) == true) {
                layer.setWindSpeed(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
            }
            if (Float.isNaN(layer.getOmega()) == true) {
                layer.setOmega(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
            }
        }
        return soundingLys;
    }

    public static List<NcSoundingLayer> removeInvalidSoundingData(
            List<NcSoundingLayer> soundingLys) {
        List<NcSoundingLayer> sndLysLst = soundingLys;
        List<Integer> removingItemList = new ArrayList<>();
        int i = 0;
        sndLysLst = replaceNanSoundingData(sndLysLst);
        for (NcSoundingLayer layer : sndLysLst) {
            if (layer.getPressure() < 0) {
                removingItemList.add(i);
            } else if (layer.getTemperature() < layer.getDewpoint()) {
                removingItemList.add(i);
            }
            i++;

        }
        // remove data with missing data
        Collections.reverse(removingItemList);
        for (i = 0; i < removingItemList.size(); i++) {
            sndLysLst.remove(removingItemList.get(i).intValue());
        }
        return sndLysLst;
    }

    public static List<NcSoundingLayer> updateObsSoundingDataForShow(
            List<NcSoundingLayer> soundingLys, float stnElv) {
        List<NcSoundingLayer> sndLysLst = soundingLys;
        List<Integer> removingItemList = new ArrayList<>();
        int i = 0;
        // reset missing data to NSHARP_NATIVE_UNVALID_DATA
        for (NcSoundingLayer layer : sndLysLst) {
            if (layer.getWindDirection() < 0)
                layer.setWindDirection(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
            if (layer.getWindSpeed() < 0)
                layer.setWindSpeed(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
            if (layer.getGeoHeight() < -50)
                layer.setGeoHeight(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
            if (layer.getPressure() < 0)
                layer.setPressure(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
            if (layer.getTemperature() < -300)
                layer.setTemperature(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
            if (layer.getDewpoint() < -300)
                layer.setDewpoint(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
            if (layer.getOmega() < -100)
                layer.setOmega(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);

            // if both pressure and height are missing, remove this layer
            if ((layer.getPressure() == NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA)
                    && (layer.getGeoHeight() == NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA))
                removingItemList.add(i);

            i++;
        }
        // remove data with missing pressure and height
        Collections.reverse(removingItemList);
        for (i = 0; i < removingItemList.size(); i++) {
            sndLysLst.remove(removingItemList.get(i).intValue());
        }

        // sort layers based on pressure, height, wind and list them from
        // highest pressure
        Collections.sort(sndLysLst, reverseHeightPressureWindComparator());

        /*
         * remove duplicate data with same pressure, or same height or pressure
         * less than 100
         */
        // create duplicate list
        removingItemList.clear();
        for (i = 0; i < sndLysLst.size() - 1; i++) {
            if (((sndLysLst.get(i).getPressure() != NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA) && (sndLysLst
                    .get(i).getPressure() == sndLysLst.get(i + 1).getPressure()))
                    || ((sndLysLst.get(i).getGeoHeight() != NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA) && (sndLysLst
                            .get(i).getGeoHeight() == sndLysLst.get(i + 1)
                            .getGeoHeight()))) {
                removingItemList.add(i + 1);
                continue;
            }
        }

        Collections.reverse(removingItemList);

        for (i = 0; i < removingItemList.size(); i++) {

            sndLysLst.remove(removingItemList.get(i).intValue());
        }

        // create 3 layer list for interpolation.
        List<NcSoundingLayer> finalLst = new ArrayList<>();
        List<NcSoundingLayer> presLst = new ArrayList<>();
        List<NcSoundingLayer> heightLst = new ArrayList<>();
        for (NcSoundingLayer layer : sndLysLst) {
            // List one, layer with both pressure and height
            if ((layer.getPressure() != NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA)
                    && (layer.getGeoHeight() != NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA)) {
                finalLst.add(0, layer);
            }
            // list two, layer with pressure only
            if ((layer.getPressure() != NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA)
                    && (layer.getGeoHeight() == NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA)) {
                presLst.add(layer);
            }
            // list three, layer with height only
            if ((layer.getPressure() == NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA)
                    && (layer.getGeoHeight() != NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA)) {
                heightLst.add(layer);
            }
        }

        // Interpolates height layer with missing pressure.
        for (NcSoundingLayer soundingLy : heightLst) {
            // round up height
            soundingLy
                    .setGeoHeight((float) Math.rint(soundingLy.getGeoHeight()));
            int index = 0;
            float pressure = NsharpLibBasics.i_pres(finalLst,
                    soundingLy.getGeoHeight());
            if (pressure != NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA) {
                soundingLy.setPressure(pressure);
                finalLst.add(index, soundingLy);
            }
        }

        Collections.sort(finalLst, reversePressureHeightWindComparator());

        // Interpolates pressure layer with missing height.
        for (NcSoundingLayer soundingLy : presLst) {
            int index = 0;
            float ht = NsharpLibBasics.i_hght(finalLst,
                    soundingLy.getPressure());
            if (ht != NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA) {
                soundingLy.setGeoHeight(ht);
                finalLst.add(index, soundingLy);
            }
        }
        Collections.sort(finalLst, reversePressureHeightWindComparator());

        /*
         * remove duplicate data with same pressure AGAIN
         */
        // create duplicate list
        removingItemList.clear();
        for (i = 0; i < finalLst.size() - 1; i++) {
            if (finalLst.get(i).getPressure() == finalLst.get(i + 1)
                    .getPressure()) {
                removingItemList.add(i + 1);
                continue;
            }
        }
        Collections.reverse(removingItemList);
        for (i = 0; i < removingItemList.size(); i++) {
            finalLst.remove(removingItemList.get(i).intValue());
        }

        // 1.Interpolates missing temperature, dew point, wind speed and
        // direction based on pressure
        // 2.remove not interpolate_able layers
        // 3. Also remove layer with pressure below 100 except 50 and 75 mb
        // layers.
        removingItemList.clear();
        i = 0;
        float interpolatedValue;
        Boolean found75 = false, found50 = false;
        for (NcSoundingLayer soundingLy : finalLst) {
            /* remove layer with pressure below 100 */
            if (soundingLy.getPressure() < 100) {
                if (soundingLy.getPressure() == 75) {
                    found75 = true;
                } else if (soundingLy.getPressure() == 50) {
                    found50 = true;
                } else {
                    removingItemList.add(i);
                    i++;
                    continue;
                }

            }

            if (soundingLy.getTemperature() == NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA) {

                interpolatedValue = NsharpLibBasics.i_temp(finalLst,
                        soundingLy.getPressure());
                if (interpolatedValue != NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA)
                    soundingLy.setTemperature(interpolatedValue);
                else {
                    // can not interpolate its temp, marked as invalid data
                    removingItemList.add(i);
                    i++;
                    continue;
                }
            }
            if (soundingLy.getDewpoint() == NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA) {
                interpolatedValue = NsharpLibBasics.i_dwpt(finalLst,
                        soundingLy.getPressure());
                if (interpolatedValue != NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA)
                    soundingLy.setDewpoint(interpolatedValue);
                else {
                    // can not interpolate its dew point, marked as invalid data
                    removingItemList.add(i);
                    i++;
                    continue;
                }
            }
            // because of i_wndu() and i_wndv() algorithm..we should call
            // i_wspd() first, then i_wdir()
            if (soundingLy.getWindSpeed() == NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA) {
                interpolatedValue = NsharpLibBasics.i_wspd(finalLst,
                        soundingLy.getPressure());
                if (interpolatedValue != NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA)
                    soundingLy.setWindSpeed(interpolatedValue);
                else {
                    // can not interpolate its wind speed, marked as invalid
                    // data
                    removingItemList.add(i);
                    i++;
                    continue;
                }
            }
            if (soundingLy.getWindDirection() == NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA) {
                interpolatedValue = NsharpLibBasics.i_wdir(finalLst,
                        soundingLy.getPressure());
                if (interpolatedValue != NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA)
                    soundingLy.setWindDirection(interpolatedValue);

                else {
                    // can not interpolate its wind direction, marked as invalid
                    // data
                    removingItemList.add(i);
                    i++;
                    continue;
                }
            }
            i++;
        }
        // remove any invalid data still having missing data or pressure below
        // 100
        Collections.reverse(removingItemList);
        for (i = 0; i < removingItemList.size(); i++) {
            finalLst.remove(removingItemList.get(i).intValue());
        }

        /*
         * Chin's NOTE: native nsharp extend sounding data to add 50 and 75 mb
         * layers. It uses 150mb layer's temp, dew, wind dir and wind speed. and
         * interpolate height only. see xtnd_sndg() at readdata.c for original c
         * code.
         */
        if (finalLst.size() > 2) {
            if (found75 == false) {
                double nm1, nm2, nm4;
                NcSoundingLayer soundingLy;
                int above1, above2;

                soundingLy = new NcSoundingLayer();
                soundingLy.setPressure(75);
                soundingLy
                        .setTemperature(NsharpLibBasics.i_temp(finalLst, 150));
                soundingLy.setDewpoint(NsharpLibBasics.i_dwpt(finalLst, 150));
                soundingLy.setWindDirection(NsharpLibBasics.i_wdir(finalLst,
                        150));
                soundingLy.setWindSpeed(NsharpLibBasics.i_wspd(finalLst, 150));
                soundingLy
                        .setOmega(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
                // interpolate height for 75mb layer
                above1 = finalLst.size() - 1;
                above2 = finalLst.size() - 2;
                nm1 = finalLst.get(above1).getGeoHeight()
                        - finalLst.get(above2).getGeoHeight();
                nm2 = Math.log(finalLst.get(above2).getPressure()
                        / finalLst.get(above1).getPressure());
                nm4 = Math.log(finalLst.get(above2).getPressure() / 75);
                soundingLy.setGeoHeight((float) (finalLst.get(above2)
                        .getGeoHeight() + ((nm4 / nm2) * nm1)));
                if (found50 == true) {
                    finalLst.add(finalLst.size() - 1, soundingLy);
                } else
                    finalLst.add(soundingLy);
            }
            if (found50 == false) {
                double nm1, nm2, nm4;
                NcSoundingLayer soundingLy;
                int above1, above2;

                soundingLy = new NcSoundingLayer();
                soundingLy.setPressure(50);
                soundingLy
                        .setTemperature(NsharpLibBasics.i_temp(finalLst, 150));
                soundingLy.setDewpoint(NsharpLibBasics.i_dwpt(finalLst, 150));
                soundingLy.setWindDirection(NsharpLibBasics.i_wdir(finalLst,
                        150));
                soundingLy.setWindSpeed(NsharpLibBasics.i_wspd(finalLst, 150));
                soundingLy
                        .setOmega(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
                // interpolate height for 50mb layer
                above1 = finalLst.size() - 1;
                above2 = finalLst.size() - 2;
                nm1 = finalLst.get(above1).getGeoHeight()
                        - finalLst.get(above2).getGeoHeight();
                nm2 = Math.log(finalLst.get(above2).getPressure()
                        / finalLst.get(above1).getPressure());
                nm4 = Math.log(finalLst.get(above2).getPressure() / 50);
                soundingLy.setGeoHeight((float) (finalLst.get(above2)
                        .getGeoHeight() + ((nm4 / nm2) * nm1)));
                finalLst.add(soundingLy);
            }
        }
        return finalLst;

    }

    // This api is called when user want to show raw data
    public static List<NcSoundingLayer> sortObsSoundingDataForShow(
            List<NcSoundingLayer> soundingLys, float stnElv) {
        List<NcSoundingLayer> sndLysLst = soundingLys;
        List<Integer> removingItemList = new ArrayList<>();
        int i = 0;
        // reset missing data to NSHARP_NATIVE_UNVALID_DATA
        for (NcSoundingLayer layer : sndLysLst) {
            if (layer.getWindDirection() < 0)
                layer.setWindDirection(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
            if (layer.getWindSpeed() < 0)
                layer.setWindSpeed(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
            if (layer.getGeoHeight() < -50)
                layer.setGeoHeight(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
            if (layer.getPressure() < 0)
                layer.setPressure(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
            if (layer.getTemperature() < -300)
                layer.setTemperature(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
            if (layer.getDewpoint() < -300)
                layer.setDewpoint(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
            if (layer.getOmega() < -100)
                layer.setOmega(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);

            i++;
        }

        // sort layers based on pressure, height, wind and list them from
        // highest pressure
        Collections.sort(sndLysLst, reversePressureComparator());

        /*
         * remove duplicate data with same pressure, or same height or pressure
         * less than 100
         */
        // create duplicate list
        removingItemList.clear();
        for (i = 0; i < sndLysLst.size() - 1; i++) {
            if (((sndLysLst.get(i).getPressure() != NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA) && (sndLysLst
                    .get(i).getPressure() == sndLysLst.get(i + 1).getPressure()))
                    || ((sndLysLst.get(i).getGeoHeight() != NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA) && (sndLysLst
                            .get(i).getGeoHeight() == sndLysLst.get(i + 1)
                            .getGeoHeight()))) {
                removingItemList.add(i + 1);
                continue;
            }
        }

        Collections.reverse(removingItemList);

        for (i = 0; i < removingItemList.size(); i++) {

            sndLysLst.remove(removingItemList.get(i).intValue());
        }
        return sndLysLst;

    }

    public static List<NcSoundingLayer> organizeSoundingDataForShow(
            List<NcSoundingLayer> soundingLys, float gndElv) {
        if (soundingLys.size() <= 0)
            return soundingLys;
        // get rid of layers with invalid data
        soundingLys = removeInvalidSoundingData(soundingLys);

        List<Integer> removingItemList = new ArrayList<>();

        // remove under ground layer(s). Note: There may be more than one
        // under ground layer.
        boolean found75 = false, found50 = false;
        for (int i = 0; i < soundingLys.size(); i++) {
            NcSoundingLayer layer = soundingLys.get(i);
            if (layer.getGeoHeight() < gndElv
                    || (layer.getPressure() < 100 && layer.getPressure() != 50f && layer
                            .getPressure() != 75f)) {
                removingItemList.add(i);
            }
            if (layer.getPressure() == 75) {
                found75 = true;// for use later
            } else if (layer.getPressure() == 50) {
                found50 = true;
            }
        }
        // remove invalid data from tail
        Collections.reverse(removingItemList);
        for (int i = 0; i < removingItemList.size(); i++) {
            soundingLys.remove(removingItemList.get(i).intValue());
        }

        // sort layers based on pressure, height, wind and list them from
        // highest pressure
        Collections.sort(soundingLys, reversePressureComparator());
        /*
         * Chin's NOTE: native nsharp extend sounding data to add 50 and 75 mb
         * layers. It uses 150mb layer's temp, dew, wind dir and wind speed. and
         * interpolate height only. see xtnd_sndg() at readdata.c for original c
         * code.
         */
        if (soundingLys.size() > 2) {
            if (found75 == false) {
                double nm1, nm2, nm4;
                NcSoundingLayer soundingLy;
                int above1, above2;

                soundingLy = new NcSoundingLayer();
                soundingLy.setPressure(75);
                soundingLy.setTemperature(NsharpLibBasics.i_temp(soundingLys,
                        150));
                soundingLy
                        .setDewpoint(NsharpLibBasics.i_dwpt(soundingLys, 150));
                soundingLy.setWindDirection(NsharpLibBasics.i_wdir(soundingLys,
                        150));
                soundingLy.setWindSpeed(NsharpLibBasics
                        .i_wspd(soundingLys, 150));
                soundingLy
                        .setOmega(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
                // interpolate height for 75mb layer
                above1 = soundingLys.size() - 1;
                above2 = soundingLys.size() - 2;
                nm1 = soundingLys.get(above1).getGeoHeight()
                        - soundingLys.get(above2).getGeoHeight();
                nm2 = Math.log(soundingLys.get(above2).getPressure()
                        / soundingLys.get(above1).getPressure());
                nm4 = Math.log(soundingLys.get(above2).getPressure() / 75);
                soundingLy.setGeoHeight((float) (soundingLys.get(above2)
                        .getGeoHeight() + ((nm4 / nm2) * nm1)));
                if (found50 == true) {
                    soundingLys.add(soundingLys.size() - 1, soundingLy);
                } else
                    soundingLys.add(soundingLy);
            }
            if (found50 == false) {
                double nm1, nm2, nm4;
                NcSoundingLayer soundingLy;
                int above1, above2;

                soundingLy = new NcSoundingLayer();
                soundingLy.setPressure(50);
                soundingLy.setTemperature(NsharpLibBasics.i_temp(soundingLys,
                        150));
                soundingLy
                        .setDewpoint(NsharpLibBasics.i_dwpt(soundingLys, 150));
                soundingLy.setWindDirection(NsharpLibBasics.i_wdir(soundingLys,
                        150));
                soundingLy.setWindSpeed(NsharpLibBasics
                        .i_wspd(soundingLys, 150));
                soundingLy
                        .setOmega(NsharpNativeConstants.NSHARP_NATIVE_INVALID_DATA);
                // interpolate height for 50mb layer
                above1 = soundingLys.size() - 1;
                above2 = soundingLys.size() - 2;
                nm1 = soundingLys.get(above1).getGeoHeight()
                        - soundingLys.get(above2).getGeoHeight();
                nm2 = Math.log(soundingLys.get(above2).getPressure()
                        / soundingLys.get(above1).getPressure());
                nm4 = Math.log(soundingLys.get(above2).getPressure() / 50);
                soundingLy.setGeoHeight((float) (soundingLys.get(above2)
                        .getGeoHeight() + ((nm4 / nm2) * nm1)));
                soundingLys.add(soundingLy);
            }
        }

        return soundingLys;
    }
}
