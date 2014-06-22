/*
 * Copyright (c) 2014 by Walter Stroebel and InfComTec.
 * All rights reserved.
 */
package nl.infcomtec.ansible;

import com.esotericsoftware.yamlbeans.YamlException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import nl.infcomtec.javahtml.JHFragment;
import nl.infcomtec.javahtml.JHParameter;

/**
 *
 * @author walter
 */
public class PlayBook {

    public final ArrayList<String> remoteUser = new ArrayList<>();
    public final File inFile;
    public final ArrayList<String> roles = new ArrayList<>();
    public final ArrayList<String> tasks = new ArrayList<>();
    public final ArrayList<String> hosts = new ArrayList<>();
    public final ArrayList<String> includes = new ArrayList<>();
    public final PlayBooks owner;

    public PlayBook(PlayBooks owner, File f, List<Map> list) throws YamlException {
        this.owner = owner;
        this.inFile = f;
        for (Map map : list) {
            {
                String s = (String) map.remove("remote_user");
                if (s != null) {
                    remoteUser.add(s);
                }
            }
            {
                List<?> rls = (List<?>) map.remove("roles");
                if (rls != null) {
                    ArrayList<String> l = new ArrayList<>();
                    for (Object o : rls) {
                        if (o instanceof String) {
                            l.add(o.toString());
                        } else if (o instanceof Map) {
                            Map<Object, Object> rmap = (Map<Object, Object>) o;
                            //System.out.println(rmap);
                            String rname = (String) rmap.remove("role");
                            l.add(rname);
                            Role parRole = owner.roles.get(rname);
                            if (parRole == null) {
                                parRole = new Role(rname);
                            }
                            AnsVariable.addOrUpdate(parRole.vars, f, rmap, null);
                            owner.roles.put(rname, parRole);
                            //System.out.println(rmap);
                        } else {
                            throw new YamlException("If not a list nor a map; what is it?");
                        }
                    }
                    roles.addAll(l);
                }
            }
            {
                ArrayList<String> a = (ArrayList<String>) map.remove("tasks");
                if (a != null) {
                    tasks.addAll(a);
                }
            }
            {
                String s = (String) map.remove("hosts");
                if (s != null) {
                    hosts.add(s);
                }
            }
            {
                String s = (String) map.remove("include");
                if (s != null) {
                    includes.add(s);
                }
            }
            if (!map.isEmpty()) {
                System.err.println("Unknown elements found in playbook "+f+" "+map);
            }
        }
    }

    public void toHtml(HttpServletRequest request, JHFragment top) {
        //System.out.println(request.getParameterMap());
        JHParameter expandP = new JHParameter(request, "expand_" + inFile.getName(), "yes");
        JHParameter collAll = new JHParameter(request, "coll_all_play");
        JHParameter expnAll = new JHParameter(request, "expn_all_play");
        if (collAll.wasSet) {
            expandP = JHParameter.overrideWasSet(expandP, false);
        }
        if (expnAll.wasSet) {
            expandP = JHParameter.overrideWasSet(expandP, true);
        }
        top.createCheckBox(expandP).appendAttr("onChange", "this.form.submit()").appendAttr("id", expandP.varName);
        if (!expandP.wasSet) {
            top.push("label").appendAttr("for", expandP.varName);
            top.appendText(" ");
            top.appendIMG("icons/application_side_expand.png");
            top.pop();
            top.appendText(" ");
        } else {
            top.push("label").appendAttr("for", expandP.varName);
            top.appendText(" ");
            top.appendIMG("icons/application_side_contract.png");
            top.pop();
            top.appendText(" ");
        }
        top.createElement("A").appendAttr("id", inFile.getName());
        JHFragment link = top.appendA("EditYml?file=" + inFile.getAbsolutePath(), "Playbook -> " + owner.shortFileName(inFile));
        top.appendAImg("DeletePlayBook?file=" + inFile.getAbsolutePath(), "icons/delete.png");
        if (!expandP.wasSet) {
            top.createElement("hr");
        } else {
            top.push("ul");
            if (!remoteUser.isEmpty()) {
                top.push("li");
                top.appendText("Remote user = " + remoteUser);
                top.pop();
            }
            if (!roles.isEmpty()) {
                top.push("li");
                top.push("table").appendAttr("border", "1");
                top.push("tr");
                top.appendTD("Role name");
                top.appendTD("Tasks");
                top.appendTD("Handlers");
                top.appendTD("Files");
                top.appendTD("Templates");
                top.appendTD("Vars");
                top.pop();
                for (String r : roles) {
                    top.push("tr");
                    Role rd = owner.roles.get(r);
                    if (rd != null) {
                        top.appendTD(rd.name);
                        top.push("td");
                        for (Task e : rd.tasks) {
                            top.appendA("EditYml?file=" + e.file.getAbsolutePath(), e.name);
                            top.createElement("br");
                        }
                        top.pop().push("td");
                        for (Map.Entry<String, RoleFileMap> e : rd.handlers.entrySet()) {
                            top.appendA("EditYml?file=" + e.getValue().file.getAbsolutePath(), e.getKey());
                            top.createElement("br");
                        }
                        top.pop().push("td");
                        for (Map.Entry<String, File> e : rd.files.entrySet()) {
                            if (UnixFile.isItASCII(e.getValue())) {
                                top.appendA("EditAny?file=" + e.getValue().getAbsolutePath(), e.getKey());
                            } else {
                                top.appendText(e.getKey());
                            }
                            top.createElement("br");
                        }
                        top.pop().push("td");
                        for (Map.Entry<String, File> e : rd.templates.entrySet()) {
                            if (UnixFile.isItASCII(e.getValue())) {
                                top.appendA("EditAny?file=" + e.getValue().getAbsolutePath(), e.getKey());
                            } else {
                                top.appendText(e.getKey());
                            }
                            top.createElement("br");
                        }
                        top.pop();
                        top.push("td");
                        for (String vn : rd.vars.keySet()) {
                            top.appendP(vn);
                        }
                        top.pop();
                    } else {
                        top.appendTD(r);
                        top.push("td").appendAttr("colspan", "5");
                        top.appendText("Role not found!");
                        top.pop();
                    }
                    top.pop();
                }
                top.pop();
                top.pop();
            }
            if (!hosts.isEmpty()) {
                top.appendLI("Hosts = " + hosts);
            }
            if (!includes.isEmpty()) {
                top.push("li");
                top.appendText("Includes");
                String sep = " = ";
                for (String e : includes) {
                    top.appendText(sep);
                    sep = ", ";
                    top.appendA("#" + e, e);
                }
                top.pop();
            }
            if (!tasks.isEmpty()) {
                top.appendLI("Tasks = " + tasks);
            }
            top.pop();
        }
    }

    @Override
    public String toString() {
        return "PlayBook{" + "remoteUser=" + remoteUser + ", inFile=" + inFile + ", roles=" + roles + ", tasks=" + tasks + ", hosts=" + hosts + ", includes=" + includes + '}';
    }

}
