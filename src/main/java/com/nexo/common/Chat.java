package com.nexo.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Una conversación: individual (2 miembros) o grupo. */
public final class Chat implements Serializable {
    private static final long serialVersionUID = 1L;

    public final String id;
    public final boolean group;
    public final String name;          // solo para grupos
    public final ArrayList<String> members;
    public final String createdBy;
    public final long createdAt;

    public Chat(String id, boolean group, String name, List<String> members, String createdBy, long createdAt) {
        this.id = id;
        this.group = group;
        this.name = name;
        this.members = new ArrayList<>(members);
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    /** En un chat individual, devuelve el otro participante. */
    public String otherMember(String me) {
        for (String m : members) {
            if (!m.equals(me)) return m;
        }
        return me;
    }
}
