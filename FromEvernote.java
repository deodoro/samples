package net.oxdf.capes.notas;

import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import com.evernote.client.android.OnClientCallback;
import com.evernote.thrift.transport.TTransportException;
import com.j256.ormlite.dao.ForeignCollection;

import net.oxdf.capes.util.DBHelper;
import net.oxdf.capes.R;
import net.oxdf.capes.util.ENHelper;
import net.oxdf.capes.util.Utils;
import net.oxdf.capes.model.Artigo;
import net.oxdf.capes.model.Nota;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by deodoro on 20/12/14.
 */
public class NotasListViewFragment  extends ListFragment {

    private int artigoId;
    private List items;
    private boolean cancelTasks = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public List makeList(ForeignCollection notas) {
        ArrayList result = new ArrayList();
        for (Nota n: notas)
            result.add(new NotasListItem(n.getPk(), n.getTitle(), n.getDate().toString()));

        return result;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        cancelTasks = true;
        super.onDestroyView();
    }

    public void loadArtigo(int id) {
        artigoId = id;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        int id = item.getItemId();

        switch (id) {
            case R.id.action_new:
                intent = new Intent(this.getView().getContext(), NotaEditActivity.class);
                startActivityForResult(intent, 0);
                return true;
            case R.id.action_share:
                if (!Utils.USE_EVERNOTE) {
                    // Somente banco de dados
                    // NÃ£o existe compartilhamento de todas as notas para EVERNOTE!
                    DBHelper helper = new DBHelper(this.getView().getContext());
                    Artigo artigo = helper.getArtigoWithId(artigoId);
                    StringBuilder text = new StringBuilder();
                    // Nota do DB
                    for(Nota n: artigo.getNotas())
                        text.append(n.getTitle() + "\n" + n.getContent() + "\n\n");

                    // Substituir por content provider?
                    intent = new Intent(Intent.ACTION_SEND);
                    intent.putExtra(Intent.EXTRA_TEXT, text.toString());
                    intent.setType("text/plain");

                    try
                    {
                        startActivity(intent);
                    }
                    catch(ActivityNotFoundException e)
                    {
                        Log.e(NotasListViewFragment.class.getName(), "Erro ao compartilhar nota", e);
                        Toast.makeText(this.getView().getContext(), "Erro ao compartilhar notas", Toast.LENGTH_LONG).show();
                    }
                }
                return true;
        }
        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_notas, menu);
        if (Utils.USE_EVERNOTE) {
            // Escondendo o item "Compartilhamento"
            // Porque nÃ£o existe compartilhamento de todas as notas com EVERNOTE
            MenuItem item = menu.findItem(R.id.action_share);
            item.setVisible(false);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 0 && resultCode == 1) {
            DBHelper helper = new DBHelper(this.getView().getContext());
            Artigo artigo = helper.getArtigoWithId(artigoId);
            String[] values = data.getStringArrayExtra("data");
            final Context context = this.getView().getContext();

            if (Utils.USE_EVERNOTE) {
                try {
                    ENHelper en = new ENHelper();
                    Nota nota = new Nota();
                    nota.setTitle(values[0]);
                    nota.setContent(values[1]);
                    en.createNote(artigo.getTruncatedTitulo(), nota, new OnClientCallback() {
                        @Override
                        public void onSuccess(NotasListItem data) {
                            Log.i(NotasListViewFragment.class.getName(), "Nova nota exportada para o Evernote");
                        }

                        @Override
                        public void onException(Exception exception) {
                            Toast.makeText(context, "Erro ao criar notas", Toast.LENGTH_LONG).show();
                        }
                    });
                }
                catch (TTransportException e) {
                    Toast.makeText(getActivity(), "Erro ao conectar com Evernote", Toast.LENGTH_SHORT).show();
                }
            }
            else {
                // Nota no banco de dados
                helper.addNota(artigo, values[0], values[1]);
            }
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        DBHelper helper = new DBHelper(this.getView().getContext());
        Artigo artigo = helper.getArtigoWithId(artigoId);
        final Context context = this.getView().getContext();

        if (Utils.USE_EVERNOTE) {
            final ProgressDialog progress = new ProgressDialog(this.getView().getContext());
            progress.setTitle("Carregando");
            progress.setMessage("Recuperando notas do Evernote...");
            progress.show();
            Log.d(NotasListViewFragment.class.getName(), "(Evernote) Carregando notas do artigo " + artigoId);
            try {
                ENHelper en = new ENHelper();
                en.loadNotesFromNotebook(artigo.getTruncatedTitulo(), new OnClientCallback>() {
                    @Override
                    public void onSuccess(List data) {
                        progress.dismiss();
                        Log.d(NotasListViewFragment.class.getName(), "Recuperados " + data.size() + " itens");
                        if (!cancelTasks)
                            setListAdapter(new NotasListAdapter(getActivity(), data));
                    }

                    @Override
                    public void onException(Exception e) {
                        progress.dismiss();
                        Log.e(NotasListViewFragment.class.getName(), "NÃ£o foi possÃ­vel carregar notas do evernote", e);
                        Toast.makeText(context, "Erro ao carregar notas", Toast.LENGTH_LONG).show();
                    }
                });
            }
            catch (TTransportException e) {
                Toast.makeText(getActivity(), "Erro ao conectar com Evernote", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Log.d(NotasListViewFragment.class.getName(), "(BD) Utilizando notas do artigo " + artigoId);
            setListAdapter(new NotasListAdapter(getActivity(), makeList(artigo.getNotas())));
        }
    }
}