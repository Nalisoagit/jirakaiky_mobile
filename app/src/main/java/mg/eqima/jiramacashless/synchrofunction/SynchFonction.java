package mg.eqima.jiramacashless.synchrofunction;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SynchFonction {
    public Map<String, Long> getDifferenceBetweenToDate(String dateCreationStringNonClean, String dateEnregistreString)
    {
        // CLEAN DE DATE:
        String dateCreationString = cleanDate(dateCreationStringNonClean);

        Map<String, Long> valiny = new HashMap();

        TimeZone timeZoneEnregistre = TimeZone.getTimeZone("Africa/Nairobi");
        SimpleDateFormat dateFormatForCreation = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat dateFormatForEnregistre = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
// set UTC
        dateFormatForCreation.setTimeZone(TimeZone.getTimeZone("UTC+03:00"));
        //dateFormat.setTimeZone(timeZoneEnregistre);
        dateFormatForEnregistre.setTimeZone(timeZoneEnregistre);
        Date dateCreation = null;
        Date dateEnregistre = null;
        try {
            dateCreation = dateFormatForCreation.parse(dateCreationString);
            dateEnregistre = dateFormatForEnregistre.parse(dateEnregistreString);
        } catch (ParseException ex) {
            Logger.getLogger(SynchFonction.class.getName()).log(Level.SEVERE, null, ex);
        }

        if(dateCreation!=null && dateEnregistre!=null)
        {
            System.out.println("Date création: "+ dateCreation);
            System.out.println("Date enregistré: "+ dateEnregistre);
            // CALCUL OF THE DIFFERENCE:
            long difference = dateCreation.getTime() - dateEnregistre.getTime();
            System.out.println("Difference: "+ difference);

            //difference = 120000;
            long min = (difference / (1000*60)) % 60;
            long seconde = (difference / 1000)% 60;
            long jour    = (difference / (1000*60*60*24)) % 365;
            long annee = (difference / (1000l*60*60*24*365));
            long heure = (difference / (1000*60*60)) % 24;
            //System.out.println("Difference en date:"+ new Date(difference));
            System.out.println("Difference en minute:"+ getPositif(min));
            System.out.println("Difference en seconde:"+ getPositif(seconde));
            System.out.println("Difference en jour:"+ getPositif(jour));
            System.out.println("Difference en annee:"+ getPositif(annee));
            System.out.println("Difference en heure:"+ getPositif(heure));

            valiny.put("minute", getPositif(min));
            valiny.put("seconde", getPositif(seconde));
            valiny.put("jour", getPositif(jour));
            valiny.put("annee", getPositif(annee));
            valiny.put("heure", getPositif(heure));

        }

        return valiny;
    }

    public Long getPositif(Long nb)
    {
        Long nbValiny = Long.valueOf(0);
        if(nb<0)
        {
            nbValiny = nb * -1;
        }
        else if(nb==0 || nb>0)
        {
            nbValiny = nb;
        }

        return nbValiny;
    }

    public String cleanDate(String date)
    {
        System.out.println("Date initiale: "+ date);
        String valiny = "";
        if(date.contains("T"))
        {
            valiny = date.replace("T", " ");
            date = date.replace("T", " ");
        }
        if(date.contains("Z"))
        {
            valiny = date.replace("Z", "");
            date = date.replace("Z", "");
        }
        else{
            valiny = date;
        }

        return valiny;
    }

    public boolean verdictDate(Map<String, Long> mapDifference)
    {
        boolean valiny = false;
        if(mapDifference.get("annee")==0)
        {
            if(mapDifference.get("jour")==0)
            {
                if(mapDifference.get("heure")==0)
                {
                    if(mapDifference.get("minute")<=5)
                    {
                        if(mapDifference.get("minute")!=0)
                        {
                            valiny = true;
                            System.out.println("Ok difference de: " + mapDifference.get("minute") + " min");
                        }
                        else if(mapDifference.get("minute")==0)
                        {
                            if(mapDifference.get("seconde")>0)
                            {
                                valiny = true;
                                System.out.println("Ok difference de: " + mapDifference.get("seconde") + " seconde");
                            }
                            else if(mapDifference.get("seconde")==0)
                            {
                                System.out.println("Mitovy exactement kosa ve eeeeh");
                            }
                        }
                    }
                    else
                    {
                        System.out.println("Trop loatra kosa: minute");
                    }
                }
                else
                {
                    System.out.println("Trop loatra kosa: heure");
                }
            }
            else
            {
                System.out.println("Trop loatra kosa: jour");
            }
        }
        else
        {
            System.out.println("Trop loatra kosa: annee");
        }

        return valiny;
    }
}
