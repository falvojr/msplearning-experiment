package br.com.cast.android.exp;

import java.util.List;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.rest.RestService;
import org.springframework.web.client.RestClientException;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.Toast;
import br.com.cast.android.exp.base.BaseActivity;
import br.com.cast.android.exp.rest.EducationalContentRestClient;
import br.com.cast.android.exp.rest.entity.EducationalContent;
import br.com.cast.android.exp.widget.EducationalListAdapter;

@EActivity(R.layout.activity_educational_content_list)
@OptionsMenu(R.menu.educational_content_list)
public class EducationalContentListActivity extends BaseActivity {

	@ViewById
	ListView listViewEducational;

	@Bean
	EducationalListAdapter educationaListAdapter;

	@RestService
	EducationalContentRestClient educationalContentRestClient;

	@AfterViews
	void tudoPronto() {
		// Ativa o meno de contexto da nossa ListVIew
		super.registerForContextMenu(listViewEducational);
		super.iniciarLoading();
		carregarListView();

	}

	@Background
	void carregarListView() {
		List<EducationalContent> educational = educationalContentRestClient
				.findByOwner(EducationalContent.ID_OWNER);
		carregarListView(educational);
	}

	@UiThread
	void carregarListView(List<EducationalContent> educational) {
		educationaListAdapter.setEducational(educational);
		listViewEducational.setAdapter(educationaListAdapter);
		educationaListAdapter.notifyDataSetChanged();
		super.terminarLoading();

	}

	/* Incluir */
	@OptionsItem(R.id.action_incluir)
	void onIncluir() {
		EducationContentActivity_.intent(this).startForResult(
				REQUESTCODE_INCLUIR);

	}

	@OnActivityResult(REQUESTCODE_INCLUIR)
	void onResultIncluir(int resultCode) {
		super.iniciarLoading();
		carregarListView();
		mostrarToastPorResultCode(resultCode, R.string.msg_sucesso_inclusao);
	}

	@OptionsItem(R.id.action_refresh)
	void onRefresh() {
		super.iniciarLoading();
		carregarListView();
	}

	/* EDITAR e EXCLUIR (CONTEXT MENU) */

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);
		super.getMenuInflater().inflate(
				R.menu.contextual_education_content_list, menu);

	}

	@Override
	public boolean onContextItemSelected(MenuItem item){
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		final EducationalContent educacionalSelecionado = educationaListAdapter.getItem(info.position);

		switch(item.getItemId()){
		case R.id.action_editar:
			// Prepara uma intenção para a UserActivity com o usuário selecionado:
			Intent intent = EducationContentActivity_.intent(this).get();
			intent.putExtra(EducationContentActivity.CHAVE_USUARIO, educacionalSelecionado);
			// Redireciona para a UserActivity esperando um resultado identificado pela chave REQUESTCODE_EDITAR:
			startActivityForResult(intent, REQUESTCODE_EDITAR);
			break;
		case R.id.action_excluir:
			// Cria um listener para a resposta positiva (Sim) na confirmação de exclusão:
			OnClickListener listenerSim = new OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					EducationalContentListActivity.super.iniciarLoading();
					deletarEducacional(educacionalSelecionado);
				}
			};
			// Exibe um alerta de confirmação usando o listener criado para o botão "Sim".
			new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setPositiveButton(R.string.sim, listenerSim)
			.setNegativeButton(R.string.nao, null)
			.setTitle(R.string.titulo_dialog_confirmacao)
			.setMessage(String.format(getString(R.string.msg_dialog_confirmacao_exclusao), educacionalSelecionado.getId()))
			.show();

			break;
		}
		return super.onContextItemSelected(item);
	}

	@OnActivityResult(REQUESTCODE_EDITAR)
	void onResultEditar(int resultCode) {
		super.iniciarLoading();
		carregarListView();

		mostrarToastPorResultCode(resultCode, R.string.msg_sucesso_edicao);
	}

	@Background
	void deletarEducacional(EducationalContent educacional) {
		try {
			educationalContentRestClient.delete(educacional.getId());

			super.iniciarLoading();
			carregarListView();

			mostrarToast(R.string.msg_sucesso_exclusao);
		} catch (RestClientException excecaoRest) {
			mostrarToast(R.string.msg_erro_rest);
		}
	}

	/*Uteis Mensagens*/
	private void mostrarToastPorResultCode(int resultCode,
			int idMensagemOk,  Object... parametros) {
		if (RESULT_OK == resultCode) {
			mostrarToast(idMensagemOk, parametros);
		}else if(RESULT_CANCELED != resultCode){
			mostrarToast(R.string.msg_erro_rest);
		}

	}

	@UiThread
	void mostrarToast(int idRecurso, Object... parametros) {
		Toast.makeText(this, getString(idRecurso, parametros), Toast.LENGTH_SHORT).show();
	}
}
