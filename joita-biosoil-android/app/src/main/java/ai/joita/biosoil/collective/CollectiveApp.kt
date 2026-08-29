package ai.joita.biosoil.collective

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

private sealed interface Screen { data object Dashboard: Screen; data object Farmers: Screen; data class Profile(val farmer: Farmer): Screen; data class History(val farmer: Farmer, val field: FarmField): Screen }

@Composable fun CollectiveApp() {
    val context = LocalContext.current
    val repo = remember { CollectiveRepository(context.applicationContext) }
    var screen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    var revision by remember { mutableIntStateOf(0) }
    var addFarmer by remember { mutableStateOf(false) }
    val refresh: () -> Unit = { revision += 1 }
    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { if (screen is Screen.Dashboard || screen is Screen.Farmers) NavigationBar {
            NavigationBarItem(screen is Screen.Dashboard, { screen=Screen.Dashboard }, { Icon(Icons.Rounded.SpaceDashboard,null) }, label={Text("Overview")})
            NavigationBarItem(screen is Screen.Farmers, { screen=Screen.Farmers }, { Icon(Icons.Rounded.Groups,null) }, label={Text("Farmers")})
        } },
        floatingActionButton = { if (screen is Screen.Dashboard || screen is Screen.Farmers) ExtendedFloatingActionButton(onClick={addFarmer=true},icon={Icon(Icons.Rounded.PersonAdd,null)},text={Text("Add farmer")}) }
    ) { padding -> key(revision) { when(val current=screen) {
        Screen.Dashboard -> Dashboard(repo,padding){screen=Screen.Farmers}
        Screen.Farmers -> FarmerList(repo,padding){screen=Screen.Profile(it)}
        is Screen.Profile -> Profile(repo,current.farmer,padding,{screen=Screen.Farmers},{f,field->screen=Screen.History(f,field)},refresh)
        is Screen.History -> History(repo,current.farmer,current.field,padding,{screen=Screen.Profile(current.farmer)},refresh)
    } } }
    if(addFarmer) FarmerForm({addFarmer=false}) { repo.addFarmer(it); addFarmer=false; refresh(); screen=Screen.Farmers }
}

@Composable private fun Dashboard(repo: CollectiveRepository,padding: PaddingValues,all:()->Unit) {
    val totals=remember{repo.totals()}; val recent=remember{repo.farmers().take(4)}
    LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item { Text("JOITA",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold); Text("Farmer Collective",style=MaterialTheme.typography.headlineMedium); Text("Field records that keep working offline",color=MaterialTheme.colorScheme.onSurfaceVariant) }
        item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { Metric("Farmers",totals.first.toString(),Icons.Rounded.Groups,Modifier.weight(1f)); Metric("Acres",acres(totals.second),Icons.Rounded.Landscape,Modifier.weight(1f)); Metric("Leads",totals.third.toString(),Icons.Rounded.Stars,Modifier.weight(1f)) } }
        item { Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)) { Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically) { Icon(Icons.Rounded.OfflineBolt,null,tint=MaterialTheme.colorScheme.primary); Column(Modifier.padding(start=12.dp)) { Text("Ready without signal",fontWeight=FontWeight.SemiBold); Text("Records and photos stay safely on this phone.",style=MaterialTheme.typography.bodySmall) } } } }
        item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) { Text("Recently added",style=MaterialTheme.typography.titleLarge); TextButton(all){Text("View all")} } }
        if(recent.isEmpty()) item { Empty("No farmers yet","Add the first farmer to begin field records.") }
        items(recent){FarmerRow(it,{})}; item{Spacer(Modifier.height(72.dp))}
    }
}

@Composable private fun Metric(label:String,value:String,icon:ImageVector,modifier:Modifier){Card(modifier){Column(Modifier.padding(12.dp)){Icon(icon,null,tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.height(10.dp));Text(value,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text(label,style=MaterialTheme.typography.bodySmall)}}}

@Composable private fun FarmerList(repo:CollectiveRepository,padding:PaddingValues,open:(Farmer)->Unit){
    var query by remember{mutableStateOf("")};var lead by remember{mutableStateOf(false)};val farmers=repo.farmers(query,lead)
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal=16.dp)){Spacer(Modifier.height(18.dp));Text("Farmers",style=MaterialTheme.typography.headlineMedium);Text("${farmers.size} records on this phone",color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(12.dp));OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),singleLine=true,label={Text("Search name, village or phone")},leadingIcon={Icon(Icons.Rounded.Search,null)});FilterChip(lead,{lead=!lead},{Text("Lead farmers")},leadingIcon={Icon(Icons.Rounded.Stars,null,Modifier.size(18.dp))});if(farmers.isEmpty())Empty("No matching farmers","Try another search or add a farmer.")else LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=96.dp)){items(farmers,key={it.id}){FarmerRow(it){open(it)}}}}
}

@Composable private fun FarmerRow(f:Farmer,open:()->Unit){Card(Modifier.fillMaxWidth().clickable(onClick=open)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(44.dp).background(MaterialTheme.colorScheme.primaryContainer,CircleShape),contentAlignment=Alignment.Center){Text(f.name.take(1).uppercase(),fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)};Column(Modifier.padding(horizontal=12.dp).weight(1f)){Row(verticalAlignment=Alignment.CenterVertically){Text(f.name,fontWeight=FontWeight.SemiBold);if(f.leadFarmer)Icon(Icons.Rounded.Stars,"Lead farmer",tint=MaterialTheme.colorScheme.tertiary,modifier=Modifier.padding(start=6.dp).size(17.dp))};Text(listOf(f.village,f.phone).filter{it.isNotBlank()}.joinToString(" · "),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};Icon(Icons.Rounded.ChevronRight,null)}}}

@Composable private fun Profile(repo:CollectiveRepository,farmer:Farmer,padding:PaddingValues,back:()->Unit,open:(Farmer,FarmField)->Unit,refresh:()->Unit){
    var add by remember{mutableStateOf(false)};val fields=remember{repo.fields(farmer.id)}
    LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Back(farmer.name,back)};item{Card{Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){if(farmer.leadFarmer)AssistChip({}, {Text("Lead farmer")},leadingIcon={Icon(Icons.Rounded.Stars,null)});Detail("Village",farmer.village);Detail("Phone",farmer.phone.ifBlank{"Not recorded"});Detail("Land",farmer.tenure);if(farmer.latitude.isNotBlank())Detail("Location","${farmer.latitude}, ${farmer.longitude}");if(farmer.notes.isNotBlank())Detail("Family & farming notes",farmer.notes)}}}
        item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column{Text("Fields",style=MaterialTheme.typography.titleLarge);Text("${fields.size} linked plots",style=MaterialTheme.typography.bodySmall)};FilledTonalButton({add=true}){Icon(Icons.Rounded.Add,null);Text("Add field")}}}
        if(fields.isEmpty())item{Empty("No fields recorded","Add acreage and crop details for this farmer.")}
        items(fields,key={it.id}){f->Card(Modifier.fillMaxWidth().clickable{open(farmer,f)}){Column(Modifier.padding(16.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(f.crop,style=MaterialTheme.typography.titleLarge);Text("${acres(f.acreage)} ac",fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)};Text(listOf(f.variety,f.season,f.soilType).filter{it.isNotBlank()}.joinToString(" · "),color=MaterialTheme.colorScheme.onSurfaceVariant);if(f.sowingDate.isNotBlank())Text("Sown ${f.sowingDate}",style=MaterialTheme.typography.bodySmall);Spacer(Modifier.height(6.dp));Text("Open field history →",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.SemiBold)}}}
    };if(add)FieldForm(farmer.id,{add=false}){repo.addField(it);add=false;refresh()}
}

@Composable private fun History(repo:CollectiveRepository,farmer:Farmer,field:FarmField,padding:PaddingValues,back:()->Unit,refresh:()->Unit){
    var add by remember{mutableStateOf(false)};val visits=remember{repo.visits(field.id)}
    LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Back(field.crop,back);Text("${farmer.name} · ${acres(field.acreage)} acres",color=MaterialTheme.colorScheme.onSurfaceVariant)}
        item{Card{Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){Detail("Variety / season",listOf(field.variety,field.season).filter{it.isNotBlank()}.joinToString(" · "));Detail("Soil / irrigation",listOf(field.soilType,field.irrigation).filter{it.isNotBlank()}.joinToString(" · ").ifBlank{"Not recorded"});if(field.inputs.isNotBlank())Detail("Inputs",field.inputs);if(field.boundaryNotes.isNotBlank())Detail("Location / boundary",field.boundaryNotes)}}}
        item{Button({add=true},Modifier.fillMaxWidth().height(52.dp)){Icon(Icons.Rounded.AddTask,null);Spacer(Modifier.width(8.dp));Text("Record field visit")}};item{Text("Visit history",style=MaterialTheme.typography.titleLarge)};if(visits.isEmpty())item{Empty("No visits yet","Record observations and recommendations from the field.")};items(visits,key={it.id}){VisitCard(it)}
    };if(add)VisitForm(repo,field.id,{add=false}){repo.addVisit(it);add=false;refresh()}
}

@Composable private fun VisitCard(v:FieldVisit){Card{Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(v.date,fontWeight=FontWeight.Bold);Text(v.cropStage,color=MaterialTheme.colorScheme.primary)};Text("Officer · ${v.officer}",style=MaterialTheme.typography.bodySmall);if(v.observations.isNotBlank())Detail("Observations",v.observations);if(v.issues.isNotBlank())Detail("Pest / disease",v.issues);if(v.recommendations.isNotBlank())Detail("Recommendations",v.recommendations);if(v.yieldData.isNotBlank())Detail("Yield / harvest",v.yieldData);if(v.notes.isNotBlank())Detail("Notes",v.notes);if(v.photoPath.isNotBlank()){val bitmap=remember(v.photoPath){android.graphics.BitmapFactory.decodeFile(v.photoPath)};if(bitmap!=null)Image(bitmap.asImageBitmap(),"Field visit photo",Modifier.fillMaxWidth().height(180.dp))}}}}
@Composable private fun Back(title:String,back:()->Unit){Row(verticalAlignment=Alignment.CenterVertically){IconButton(back){Icon(Icons.Rounded.ArrowBack,"Back")};Text(title,style=MaterialTheme.typography.headlineMedium,maxLines=1,overflow=TextOverflow.Ellipsis)}}
@Composable private fun Detail(label:String,value:String){Column{Text(label.uppercase(),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(value)}}
@Composable private fun Empty(title:String,body:String){Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)){Column(Modifier.fillMaxWidth().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(title,fontWeight=FontWeight.Bold);Text(body,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}

@Composable private fun FarmerForm(close:()->Unit,save:(Farmer)->Unit){var name by remember{mutableStateOf("")};var phone by remember{mutableStateOf("")};var village by remember{mutableStateOf("")};var lat by remember{mutableStateOf("")};var lng by remember{mutableStateOf("")};var lead by remember{mutableStateOf(false)};var notes by remember{mutableStateOf("")};var tenure by remember{mutableStateOf("Owned")};Form("Add farmer",close,name.isNotBlank()&&village.isNotBlank(),{save(Farmer(name=name,phone=phone,village=village,latitude=lat,longitude=lng,leadFarmer=lead,notes=notes,tenure=tenure))}){Field("Farmer name *",name){name=it};Field("Phone",phone){phone=it};Field("Village *",village){village=it};Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.weight(1f)){Field("GPS latitude",lat){lat=it}};Box(Modifier.weight(1f)){Field("Longitude",lng){lng=it}}};Choices("Land arrangement",listOf("Owned","Tenant","Shared"),tenure){tenure=it};Row(verticalAlignment=Alignment.CenterVertically){Switch(lead,{lead=it});Text("Lead farmer",Modifier.padding(start=8.dp))};Field("Family & farming notes",notes,3){notes=it}}}
@Composable private fun FieldForm(farmerId:Long,close:()->Unit,save:(FarmField)->Unit){var area by remember{mutableStateOf("")};var crop by remember{mutableStateOf("")};var variety by remember{mutableStateOf("")};var season by remember{mutableStateOf("Kharif")};var sowing by remember{mutableStateOf("")};var soil by remember{mutableStateOf("")};var irrigation by remember{mutableStateOf("")};var inputs by remember{mutableStateOf("")};var boundary by remember{mutableStateOf("")};Form("Add field",close,crop.isNotBlank()&&(area.toDoubleOrNull()?:0.0)>0,{save(FarmField(farmerId=farmerId,acreage=area.toDouble(),crop=crop,variety=variety,season=season,sowingDate=sowing,soilType=soil,irrigation=irrigation,inputs=inputs,boundaryNotes=boundary))}){Field("Acreage *",area){area=it};Field("Crop *",crop){crop=it};Field("Variety",variety){variety=it};Choices("Season",listOf("Kharif","Rabi","Zaid"),season){season=it};Field("Sowing date (DD-MM-YYYY)",sowing){sowing=it};Field("Soil type",soil){soil=it};Field("Irrigation",irrigation){irrigation=it};Field("Inputs used",inputs,2){inputs=it};Field("Location / boundary notes",boundary,3){boundary=it}}}
@Composable private fun VisitForm(repo:CollectiveRepository,fieldId:Long,close:()->Unit,save:(FieldVisit)->Unit){val context=LocalContext.current;var date by remember{mutableStateOf(SimpleDateFormat("dd-MM-yyyy",Locale.US).format(Date()))};var officer by remember{mutableStateOf("")};var stage by remember{mutableStateOf("")};var observations by remember{mutableStateOf("")};var issues by remember{mutableStateOf("")};var recs by remember{mutableStateOf("")};var yield by remember{mutableStateOf("")};var notes by remember{mutableStateOf("")};var photo by remember{mutableStateOf("")};val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri:Uri?->uri?.let{context.contentResolver.openInputStream(it)?.use{s->photo=repo.savePhoto(s.readBytes())}}};Form("Record visit",close,date.isNotBlank()&&officer.isNotBlank(),{save(FieldVisit(fieldId=fieldId,date=date,officer=officer,cropStage=stage,observations=observations,issues=issues,recommendations=recs,yieldData=yield,notes=notes,photoPath=photo))}){Field("Visit date *",date){date=it};Field("Field officer *",officer){officer=it};Field("Crop stage",stage){stage=it};Field("Observations",observations,3){observations=it};Field("Pest / disease issues",issues,3){issues=it};Field("Recommendations",recs,3){recs=it};Field("Yield / harvest data",yield,2){yield=it};Field("Additional notes",notes,2){notes=it};OutlinedButton({picker.launch("image/*")},Modifier.fillMaxWidth()){Icon(if(photo.isBlank())Icons.Rounded.AddAPhoto else Icons.Rounded.CheckCircle,null);Spacer(Modifier.width(8.dp));Text(if(photo.isBlank())"Add field photo" else "Photo saved on device")}}}
@Composable private fun Form(title:String,close:()->Unit,valid:Boolean,save:()->Unit,content:@Composable ColumnScope.()->Unit){AlertDialog(onDismissRequest=close,title={Text(title)},text={LazyColumn(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Column(verticalArrangement=Arrangement.spacedBy(10.dp),content=content)}}},confirmButton={Button(save,enabled=valid){Text("Save")}},dismissButton={TextButton(close){Text("Cancel")}})}
@Composable private fun Field(label:String,value:String,lines:Int=1,change:(String)->Unit){OutlinedTextField(value,change,Modifier.fillMaxWidth(),label={Text(label)},singleLine=lines==1,minLines=lines)}
@Composable private fun Choices(label:String,values:List<String>,selected:String,choose:(String)->Unit){Column{Text(label,style=MaterialTheme.typography.labelMedium);Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){values.forEach{FilterChip(selected==it,{choose(it)},{Text(it)})}}}}
private fun acres(value:Double)=if(value%1.0==0.0)value.toInt().toString()else String.format(Locale.US,"%.1f",value)
