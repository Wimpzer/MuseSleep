package com.musesleep.musesleep;

import com.choosemuse.libmuse.Muse;

import java.io.Serializable;
import java.util.List;

public class PairedMuses implements Serializable {

    List<Muse> pairedMuses;

    public PairedMuses(List<Muse> pairedMuses) {
        this.pairedMuses = pairedMuses;
    }

    public List<Muse> getPairedMuses() {
        return pairedMuses;
    }

    public void setPairedMuses(List<Muse> pairedMuses) {
        this.pairedMuses = pairedMuses;
    }
}
