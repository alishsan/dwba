// Compiled by ClojureScript 1.11.60 {:optimizations :none}
goog.provide('dwba_web.dashboard');
goog.require('cljs.core');
goog.require('goog.dom');
goog.require('goog.events');
goog.require('goog.dom.forms');
goog.require('clojure.string');
dwba_web.dashboard.plotly = Plotly;
if((typeof dwba_web !== 'undefined') && (typeof dwba_web.dashboard !== 'undefined') && (typeof dwba_web.dashboard.dashboard_state !== 'undefined')){
} else {
dwba_web.dashboard.dashboard_state = cljs.core.atom.call(null,new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"api-base","api-base",891294827),"",new cljs.core.Keyword(null,"current-data","current-data",-2102182813),null], null));
}
dwba_web.dashboard.get_element = (function dwba_web$dashboard$get_element(id){
return goog.dom.getElement(id);
});
dwba_web.dashboard.get_value = (function dwba_web$dashboard$get_value(id){
return goog.dom.forms.getValue(dwba_web.dashboard.get_element.call(null,id));
});
dwba_web.dashboard.set_value_BANG_ = (function dwba_web$dashboard$set_value_BANG_(id,value){
return goog.dom.forms.setValue(dwba_web.dashboard.get_element.call(null,id),value);
});
dwba_web.dashboard.set_text_BANG_ = (function dwba_web$dashboard$set_text_BANG_(id,text){
return (dwba_web.dashboard.get_element.call(null,id).textContent = text);
});
dwba_web.dashboard.get_float = (function dwba_web$dashboard$get_float(id){
return parseFloat(dwba_web.dashboard.get_value.call(null,id));
});
dwba_web.dashboard.get_int = (function dwba_web$dashboard$get_int(id){
return parseInt(dwba_web.dashboard.get_value.call(null,id));
});
dwba_web.dashboard.parse_comma_separated = (function dwba_web$dashboard$parse_comma_separated(s){
return cljs.core.filter.call(null,cljs.core.complement.call(null,clojure.string.blank_QMARK_),cljs.core.map.call(null,clojure.string.trim,clojure.string.split.call(null,s,/,/)));
});
dwba_web.dashboard.initialize_event_listeners = (function dwba_web$dashboard$initialize_event_listeners(){
var seq__1287_1291 = cljs.core.seq.call(null,new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, ["V0","R0","a0","radius"], null));
var chunk__1288_1292 = null;
var count__1289_1293 = (0);
var i__1290_1294 = (0);
while(true){
if((i__1290_1294 < count__1289_1293)){
var param_1295 = cljs.core._nth.call(null,chunk__1288_1292,i__1290_1294);
var slider_1296 = dwba_web.dashboard.get_element.call(null,param_1295);
var value_display_1297 = dwba_web.dashboard.get_element.call(null,[cljs.core.str.cljs$core$IFn$_invoke$arity$1(param_1295),"-value"].join(''));
goog.events.listen(slider_1296,"input",((function (seq__1287_1291,chunk__1288_1292,count__1289_1293,i__1290_1294,slider_1296,value_display_1297,param_1295){
return (function (e){
var value = parseFloat(e.target.value);
var unit = ((cljs.core._EQ_.call(null,param_1295,"V0"))?"MeV":"fm");
return dwba_web.dashboard.set_text_BANG_.call(null,[cljs.core.str.cljs$core$IFn$_invoke$arity$1(param_1295),"-value"].join(''),[cljs.core.str.cljs$core$IFn$_invoke$arity$1(value)," ",unit].join(''));
});})(seq__1287_1291,chunk__1288_1292,count__1289_1293,i__1290_1294,slider_1296,value_display_1297,param_1295))
);


var G__1298 = seq__1287_1291;
var G__1299 = chunk__1288_1292;
var G__1300 = count__1289_1293;
var G__1301 = (i__1290_1294 + (1));
seq__1287_1291 = G__1298;
chunk__1288_1292 = G__1299;
count__1289_1293 = G__1300;
i__1290_1294 = G__1301;
continue;
} else {
var temp__5823__auto___1302 = cljs.core.seq.call(null,seq__1287_1291);
if(temp__5823__auto___1302){
var seq__1287_1303__$1 = temp__5823__auto___1302;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__1287_1303__$1)){
var c__5568__auto___1304 = cljs.core.chunk_first.call(null,seq__1287_1303__$1);
var G__1305 = cljs.core.chunk_rest.call(null,seq__1287_1303__$1);
var G__1306 = c__5568__auto___1304;
var G__1307 = cljs.core.count.call(null,c__5568__auto___1304);
var G__1308 = (0);
seq__1287_1291 = G__1305;
chunk__1288_1292 = G__1306;
count__1289_1293 = G__1307;
i__1290_1294 = G__1308;
continue;
} else {
var param_1309 = cljs.core.first.call(null,seq__1287_1303__$1);
var slider_1310 = dwba_web.dashboard.get_element.call(null,param_1309);
var value_display_1311 = dwba_web.dashboard.get_element.call(null,[cljs.core.str.cljs$core$IFn$_invoke$arity$1(param_1309),"-value"].join(''));
goog.events.listen(slider_1310,"input",((function (seq__1287_1291,chunk__1288_1292,count__1289_1293,i__1290_1294,slider_1310,value_display_1311,param_1309,seq__1287_1303__$1,temp__5823__auto___1302){
return (function (e){
var value = parseFloat(e.target.value);
var unit = ((cljs.core._EQ_.call(null,param_1309,"V0"))?"MeV":"fm");
return dwba_web.dashboard.set_text_BANG_.call(null,[cljs.core.str.cljs$core$IFn$_invoke$arity$1(param_1309),"-value"].join(''),[cljs.core.str.cljs$core$IFn$_invoke$arity$1(value)," ",unit].join(''));
});})(seq__1287_1291,chunk__1288_1292,count__1289_1293,i__1290_1294,slider_1310,value_display_1311,param_1309,seq__1287_1303__$1,temp__5823__auto___1302))
);


var G__1312 = cljs.core.next.call(null,seq__1287_1303__$1);
var G__1313 = null;
var G__1314 = (0);
var G__1315 = (0);
seq__1287_1291 = G__1312;
chunk__1288_1292 = G__1313;
count__1289_1293 = G__1314;
i__1290_1294 = G__1315;
continue;
}
} else {
}
}
break;
}

var calc_btn_1316 = dwba_web.dashboard.get_element.call(null,"calculate-btn");
goog.events.listen(calc_btn_1316,"click",dwba_web.dashboard.calculate_dwba);

var reset_btn = dwba_web.dashboard.get_element.call(null,"reset-btn");
if(cljs.core.truth_(reset_btn)){
return goog.events.listen(reset_btn,"click",dwba_web.dashboard.reset_parameters);
} else {
return null;
}
});
dwba_web.dashboard.load_default_parameters = (function dwba_web$dashboard$load_default_parameters(){
var api_base = new cljs.core.Keyword(null,"api-base","api-base",891294827).cljs$core$IFn$_invoke$arity$1(cljs.core.deref.call(null,dwba_web.dashboard.dashboard_state));
return fetch([cljs.core.str.cljs$core$IFn$_invoke$arity$1(api_base),"/api/parameters"].join('')).then((function (response){
return response.json();
})).then((function (data){
if(cljs.core.truth_((data["default_parameters"]))){
return dwba_web.dashboard.set_parameters.call(null,cljs.core.js__GT_clj.call(null,(data["default_parameters"]),new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true));
} else {
return null;
}
})).catch((function (error){
return console.error("Error loading default parameters:",error);
}));
});
dwba_web.dashboard.set_parameters = (function dwba_web$dashboard$set_parameters(params){
dwba_web.dashboard.set_value_BANG_.call(null,"V0",new cljs.core.Keyword(null,"V0","V0",-401008246).cljs$core$IFn$_invoke$arity$1(params));

dwba_web.dashboard.set_value_BANG_.call(null,"R0",new cljs.core.Keyword(null,"R0","R0",1568258852).cljs$core$IFn$_invoke$arity$1(params));

dwba_web.dashboard.set_value_BANG_.call(null,"a0",new cljs.core.Keyword(null,"a0","a0",688597649).cljs$core$IFn$_invoke$arity$1(params));

dwba_web.dashboard.set_value_BANG_.call(null,"radius",new cljs.core.Keyword(null,"radius","radius",-2073122258).cljs$core$IFn$_invoke$arity$1(params));

dwba_web.dashboard.set_value_BANG_.call(null,"energy-range",clojure.string.join.call(null,",",new cljs.core.Keyword(null,"energies","energies",-1165472800).cljs$core$IFn$_invoke$arity$1(params)));

dwba_web.dashboard.set_value_BANG_.call(null,"L-values",clojure.string.join.call(null,",",new cljs.core.Keyword(null,"L-values","L-values",-907840386).cljs$core$IFn$_invoke$arity$1(params)));

if(cljs.core.truth_(new cljs.core.Keyword(null,"E_ex","E_ex",564024644).cljs$core$IFn$_invoke$arity$1(params))){
dwba_web.dashboard.set_value_BANG_.call(null,"E_ex",new cljs.core.Keyword(null,"E_ex","E_ex",564024644).cljs$core$IFn$_invoke$arity$1(params));
} else {
}

if(cljs.core.truth_(new cljs.core.Keyword(null,"lambda","lambda",-1483427225).cljs$core$IFn$_invoke$arity$1(params))){
dwba_web.dashboard.set_value_BANG_.call(null,"lambda",new cljs.core.Keyword(null,"lambda","lambda",-1483427225).cljs$core$IFn$_invoke$arity$1(params));
} else {
}

if(cljs.core.truth_(new cljs.core.Keyword(null,"beta","beta",455605892).cljs$core$IFn$_invoke$arity$1(params))){
dwba_web.dashboard.set_value_BANG_.call(null,"beta",new cljs.core.Keyword(null,"beta","beta",455605892).cljs$core$IFn$_invoke$arity$1(params));
} else {
}

if(cljs.core.truth_(new cljs.core.Keyword(null,"reaction_type","reaction_type",1576322226).cljs$core$IFn$_invoke$arity$1(params))){
dwba_web.dashboard.set_value_BANG_.call(null,"reaction_type",new cljs.core.Keyword(null,"reaction_type","reaction_type",1576322226).cljs$core$IFn$_invoke$arity$1(params));
} else {
}

dwba_web.dashboard.set_text_BANG_.call(null,"V0-value",[cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"V0","V0",-401008246).cljs$core$IFn$_invoke$arity$1(params))," MeV"].join(''));

dwba_web.dashboard.set_text_BANG_.call(null,"R0-value",[cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"R0","R0",1568258852).cljs$core$IFn$_invoke$arity$1(params))," fm"].join(''));

dwba_web.dashboard.set_text_BANG_.call(null,"a0-value",[cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"a0","a0",688597649).cljs$core$IFn$_invoke$arity$1(params))," fm"].join(''));

return dwba_web.dashboard.set_text_BANG_.call(null,"radius-value",[cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"radius","radius",-2073122258).cljs$core$IFn$_invoke$arity$1(params))," fm"].join(''));
});
dwba_web.dashboard.get_parameters = (function dwba_web$dashboard$get_parameters(){
return cljs.core.PersistentHashMap.fromArrays([new cljs.core.Keyword(null,"energies","energies",-1165472800),new cljs.core.Keyword(null,"E_ex","E_ex",564024644),new cljs.core.Keyword(null,"beta","beta",455605892),new cljs.core.Keyword(null,"R0","R0",1568258852),new cljs.core.Keyword(null,"lambda","lambda",-1483427225),new cljs.core.Keyword(null,"V0","V0",-401008246),new cljs.core.Keyword(null,"radius","radius",-2073122258),new cljs.core.Keyword(null,"a0","a0",688597649),new cljs.core.Keyword(null,"reaction_type","reaction_type",1576322226),new cljs.core.Keyword(null,"L-values","L-values",-907840386)],[dwba_web.dashboard.parse_comma_separated.call(null,dwba_web.dashboard.get_value.call(null,"energy-range")),dwba_web.dashboard.get_float.call(null,"E_ex"),dwba_web.dashboard.get_float.call(null,"beta"),dwba_web.dashboard.get_float.call(null,"R0"),dwba_web.dashboard.get_int.call(null,"lambda"),dwba_web.dashboard.get_float.call(null,"V0"),dwba_web.dashboard.get_float.call(null,"radius"),dwba_web.dashboard.get_float.call(null,"a0"),dwba_web.dashboard.get_value.call(null,"reaction_type"),dwba_web.dashboard.parse_comma_separated.call(null,dwba_web.dashboard.get_value.call(null,"L-values"))]);
});
dwba_web.dashboard.show_status = (function dwba_web$dashboard$show_status(message,type){
var status_div = dwba_web.dashboard.get_element.call(null,"status-messages");
var alert_class = (function (){var G__1317 = type;
switch (G__1317) {
case "error":
return "error";

break;
case "success":
return "success";

break;
default:
return "alert-info";

}
})();
var icon_class = (function (){var G__1318 = type;
switch (G__1318) {
case "error":
return "exclamation-triangle";

break;
case "success":
return "check-circle";

break;
default:
return "info-circle";

}
})();
(status_div.innerHTML = ["<div class=\"",cljs.core.str.cljs$core$IFn$_invoke$arity$1(alert_class),"\">","<i class=\"fas fa-",cljs.core.str.cljs$core$IFn$_invoke$arity$1(icon_class),"\"></i> ",cljs.core.str.cljs$core$IFn$_invoke$arity$1(message),"</div>"].join(''));

return setTimeout((function (){
return (status_div.innerHTML = "");
}),(5000));
});
dwba_web.dashboard.calculate_dwba = (function dwba_web$dashboard$calculate_dwba(){
var start_time = Date.now();
var calculate_btn = dwba_web.dashboard.get_element.call(null,"calculate-btn");
var api_base = new cljs.core.Keyword(null,"api-base","api-base",891294827).cljs$core$IFn$_invoke$arity$1(cljs.core.deref.call(null,dwba_web.dashboard.dashboard_state));
(calculate_btn.disabled = true);

(calculate_btn.innerHTML = "<i class=\"fas fa-spinner fa-spin\"></i> Calculating...");

dwba_web.dashboard.show_status.call(null,"Performing DWBA calculations...","info");

var params = dwba_web.dashboard.get_parameters.call(null);
if(((cljs.core.empty_QMARK_.call(null,new cljs.core.Keyword(null,"energies","energies",-1165472800).cljs$core$IFn$_invoke$arity$1(params))) || (cljs.core.empty_QMARK_.call(null,new cljs.core.Keyword(null,"L-values","L-values",-907840386).cljs$core$IFn$_invoke$arity$1(params))))){
dwba_web.dashboard.show_status.call(null,"Error: Please provide valid energy range and angular momenta","error");

(calculate_btn.disabled = false);

return (calculate_btn.innerHTML = "<i class=\"fas fa-calculator\"></i> Calculate DWBA");
} else {
var basic_promise = fetch([cljs.core.str.cljs$core$IFn$_invoke$arity$1(api_base),"/api/calculate"].join(''),cljs.core.clj__GT_js.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"method","method",55703592),"POST",new cljs.core.Keyword(null,"headers","headers",-835030129),new cljs.core.PersistentArrayMap(null, 1, ["Content-Type","application/json"], null),new cljs.core.Keyword(null,"body","body",-2049205669),JSON.stringify(cljs.core.clj__GT_js.call(null,params))], null))).then((function (r){
return r.json();
}));
var elastic_promise = fetch([cljs.core.str.cljs$core$IFn$_invoke$arity$1(api_base),"/api/elastic"].join(''),cljs.core.clj__GT_js.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"method","method",55703592),"POST",new cljs.core.Keyword(null,"headers","headers",-835030129),new cljs.core.PersistentArrayMap(null, 1, ["Content-Type","application/json"], null),new cljs.core.Keyword(null,"body","body",-2049205669),JSON.stringify(cljs.core.clj__GT_js.call(null,params))], null))).then((function (r){
return r.json();
}));
var inelastic_promise = fetch([cljs.core.str.cljs$core$IFn$_invoke$arity$1(api_base),"/api/inelastic"].join(''),cljs.core.clj__GT_js.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"method","method",55703592),"POST",new cljs.core.Keyword(null,"headers","headers",-835030129),new cljs.core.PersistentArrayMap(null, 1, ["Content-Type","application/json"], null),new cljs.core.Keyword(null,"body","body",-2049205669),JSON.stringify(cljs.core.clj__GT_js.call(null,params))], null))).then((function (r){
return r.json();
}));
var transfer_promise = fetch([cljs.core.str.cljs$core$IFn$_invoke$arity$1(api_base),"/api/transfer"].join(''),cljs.core.clj__GT_js.call(null,new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"method","method",55703592),"POST",new cljs.core.Keyword(null,"headers","headers",-835030129),new cljs.core.PersistentArrayMap(null, 1, ["Content-Type","application/json"], null),new cljs.core.Keyword(null,"body","body",-2049205669),JSON.stringify(cljs.core.clj__GT_js.call(null,params))], null))).then((function (r){
return r.json();
}));
return Promise.all([basic_promise,elastic_promise,inelastic_promise,transfer_promise]).then((function (results){
var calculation_time = (Date.now() - start_time);
var basic_result = (results[(0)]);
var elastic_result = (results[(1)]);
var inelastic_result = (results[(2)]);
var transfer_result = (results[(3)]);
if(cljs.core.truth_((basic_result["success"]))){
cljs.core.swap_BANG_.call(null,dwba_web.dashboard.dashboard_state,cljs.core.assoc,new cljs.core.Keyword(null,"current-data","current-data",-2102182813),cljs.core.merge.call(null,cljs.core.js__GT_clj.call(null,(basic_result["data"]),new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true),(cljs.core.truth_((elastic_result["success"]))?new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"elastic","elastic",6164687),cljs.core.js__GT_clj.call(null,(elastic_result["data"]["elastic"]),new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true)], null):null),(cljs.core.truth_((inelastic_result["success"]))?new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"inelastic","inelastic",-1428247471),cljs.core.js__GT_clj.call(null,(inelastic_result["data"]["inelastic"]),new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true)], null):null),(cljs.core.truth_((transfer_result["success"]))?new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"transfer","transfer",327423400),cljs.core.js__GT_clj.call(null,(transfer_result["data"]["transfer"]),new cljs.core.Keyword(null,"keywordize-keys","keywordize-keys",1310784252),true)], null):null)));

dwba_web.dashboard.update_all_plots.call(null);

dwba_web.dashboard.update_dashboard_stats.call(null,calculation_time);

return dwba_web.dashboard.show_status.call(null,["Calculation completed successfully in ",cljs.core.str.cljs$core$IFn$_invoke$arity$1(calculation_time),"ms"].join(''),"success");
} else {
dwba_web.dashboard.show_status.call(null,["Error: ",cljs.core.str.cljs$core$IFn$_invoke$arity$1((function (){var or__5045__auto__ = (basic_result["error"]);
if(cljs.core.truth_(or__5045__auto__)){
return or__5045__auto__;
} else {
return "Calculation failed";
}
})())].join(''),"error");

(calculate_btn.disabled = false);

return (calculate_btn.innerHTML = "<i class=\"fas fa-calculator\"></i> Calculate DWBA");
}
})).catch((function (error){
console.error("Calculation error:",error);

return dwba_web.dashboard.show_status.call(null,["Error: ",cljs.core.str.cljs$core$IFn$_invoke$arity$1(error.message)].join(''),"error");
})).finally((function (){
(calculate_btn.disabled = false);

return (calculate_btn.innerHTML = "<i class=\"fas fa-calculator\"></i> Calculate DWBA");
}));
}
});
dwba_web.dashboard.update_all_plots = (function dwba_web$dashboard$update_all_plots(){
var current_data = new cljs.core.Keyword(null,"current-data","current-data",-2102182813).cljs$core$IFn$_invoke$arity$1(cljs.core.deref.call(null,dwba_web.dashboard.dashboard_state));
if(cljs.core.truth_(current_data)){
dwba_web.dashboard.plot_phase_shifts.call(null);

dwba_web.dashboard.plot_r_matrices.call(null);

dwba_web.dashboard.plot_potentials.call(null);

dwba_web.dashboard.plot_cross_sections.call(null);

if(cljs.core.truth_(new cljs.core.Keyword(null,"elastic","elastic",6164687).cljs$core$IFn$_invoke$arity$1(current_data))){
dwba_web.dashboard.plot_elastic.call(null);
} else {
}

if(cljs.core.truth_(new cljs.core.Keyword(null,"inelastic","inelastic",-1428247471).cljs$core$IFn$_invoke$arity$1(current_data))){
dwba_web.dashboard.plot_inelastic.call(null);
} else {
}

if(cljs.core.truth_(new cljs.core.Keyword(null,"transfer","transfer",327423400).cljs$core$IFn$_invoke$arity$1(current_data))){
dwba_web.dashboard.plot_transfer.call(null);
} else {
}

return dwba_web.dashboard.plot_dashboard.call(null);
} else {
return null;
}
});
dwba_web.dashboard.plot_phase_shifts = (function dwba_web$dashboard$plot_phase_shifts(){
var data = new cljs.core.Keyword(null,"phase-shifts","phase-shifts",1478143705).cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"current-data","current-data",-2102182813).cljs$core$IFn$_invoke$arity$1(cljs.core.deref.call(null,dwba_web.dashboard.dashboard_state)));
var traces = cljs.core.reduce.call(null,(function (traces,point){
var L = new cljs.core.Keyword(null,"L","L",-1038307519).cljs$core$IFn$_invoke$arity$1(point);
return cljs.core.update.call(null,traces,L,(function (trace){
if(cljs.core.truth_(trace)){
return cljs.core.update.call(null,cljs.core.update.call(null,trace,new cljs.core.Keyword(null,"x","x",2099068185),cljs.core.conj,new cljs.core.Keyword(null,"energy","energy",129856526).cljs$core$IFn$_invoke$arity$1(point)),new cljs.core.Keyword(null,"y","y",-1757859776),cljs.core.conj,(new cljs.core.Keyword(null,"phase-shift","phase-shift",-353708650).cljs$core$IFn$_invoke$arity$1(point) * ((180) / Math.PI)));
} else {
return new cljs.core.PersistentArrayMap(null, 7, [new cljs.core.Keyword(null,"x","x",2099068185),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"energy","energy",129856526).cljs$core$IFn$_invoke$arity$1(point)], null),new cljs.core.Keyword(null,"y","y",-1757859776),new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [(new cljs.core.Keyword(null,"phase-shift","phase-shift",-353708650).cljs$core$IFn$_invoke$arity$1(point) * ((180) / Math.PI))], null),new cljs.core.Keyword(null,"name","name",1843675177),["L = ",cljs.core.str.cljs$core$IFn$_invoke$arity$1(L)].join(''),new cljs.core.Keyword(null,"type","type",1174270348),"scatter",new cljs.core.Keyword(null,"mode","mode",654403691),"lines+markers",new cljs.core.Keyword(null,"line","line",212345235),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"width","width",-384071477),(3)], null),new cljs.core.Keyword(null,"marker","marker",865118313),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"size","size",1098693007),(6)], null)], null);
}
}));
}),cljs.core.PersistentArrayMap.EMPTY,data);
var plot_data = cljs.core.clj__GT_js.call(null,cljs.core.vals.call(null,traces));
var layout = cljs.core.clj__GT_js.call(null,new cljs.core.PersistentArrayMap(null, 8, [new cljs.core.Keyword(null,"title","title",636505583),"Nuclear Phase Shifts vs Energy",new cljs.core.Keyword(null,"xaxis","xaxis",488378734),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"title","title",636505583),"Energy (MeV)",new cljs.core.Keyword(null,"gridcolor","gridcolor",-145824598),"#e0e0e0"], null),new cljs.core.Keyword(null,"yaxis","yaxis",-1783465932),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"title","title",636505583),"Phase Shift (degrees)",new cljs.core.Keyword(null,"gridcolor","gridcolor",-145824598),"#e0e0e0"], null),new cljs.core.Keyword(null,"plot_bgcolor","plot_bgcolor",-1887651068),"rgba(0,0,0,0)",new cljs.core.Keyword(null,"paper_bgcolor","paper_bgcolor",-1509275419),"rgba(0,0,0,0)",new cljs.core.Keyword(null,"font","font",-1506159249),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"family","family",-1313145692),"Arial, sans-serif"], null),new cljs.core.Keyword(null,"legend","legend",-1027192245),new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null,"x","x",2099068185),0.02,new cljs.core.Keyword(null,"y","y",-1757859776),0.98], null),new cljs.core.Keyword(null,"margin","margin",-995903681),new cljs.core.PersistentArrayMap(null, 4, [new cljs.core.Keyword(null,"t","t",-1397832519),(50),new cljs.core.Keyword(null,"b","b",1482224470),(50),new cljs.core.Keyword(null,"l","l",1395893423),(60),new cljs.core.Keyword(null,"r","r",-471384190),(30)], null)], null));
return dwba_web.dashboard.plotly.newPlot("phase-plot",plot_data,layout,cljs.core.clj__GT_js.call(null,new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"responsive","responsive",-1606632318),true], null)));
});
