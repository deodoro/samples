package net.oxdf.capes.tags;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.TextView;

import com.j256.ormlite.dao.ForeignCollection;

import net.oxdf.capes.util.DBHelper;
import net.oxdf.capes.R;
import net.oxdf.capes.model.Artigo;
import net.oxdf.capes.model.Tag;
import net.oxdf.capes.model.TagArtigo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by deodoro on 05/01/15.
 */
public class TagsAdapter extends BaseAdapter implements Filterable {

    private Artigo artigo;
    private List rawList;
    private List filteredList;
    private Filter filter;
    private Context context;
    private DBHelper helper;
    private ISearchFragment parentSearch;

    public TagsAdapter(Context context, Artigo artigo, ISearchFragment parent) {
        this.context = context;
        this.rawList = makeList(artigo.getTags());
        this.filteredList = this.rawList;
        this.helper = new DBHelper(context);
        this.artigo = artigo;
        this.parentSearch = parent;
        getFilter();
    }

    public List makeList(ForeignCollection tags) {
        ArrayList result = new ArrayList();
        for (TagArtigo t: tags)
            result.add(new TagsListItem(t.getPk(), t.getTag().getTag()));

        return result;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.listitem_tags, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.tvTitle = (TextView)convertView.findViewById(R.id.tvTitle);
            viewHolder.btAdd = (ImageButton)convertView.findViewById(R.id.bt_addTag);
            viewHolder.btRemove = (ImageButton)convertView.findViewById(R.id.bt_removeTag);
            convertView.setTag(viewHolder);
        }
        else
            viewHolder = (ViewHolder)convertView.getTag();

        TagsListItem item = getItem(position);
        viewHolder.tvTitle.setText(item.getTag());
        if (item.isNew()) {
            viewHolder.btRemove.setVisibility(View.INVISIBLE);
            viewHolder.btAdd.setVisibility(View.VISIBLE);
        }
        else {
            viewHolder.btRemove.setVisibility(View.VISIBLE);
            viewHolder.btAdd.setVisibility(View.INVISIBLE);
        }

        viewHolder.btAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewGroup parent = (ViewGroup) v.getParent();
                ImageButton btAdd = (ImageButton)parent.findViewById(R.id.bt_addTag);
                ImageButton btRemove = (ImageButton)parent.findViewById(R.id.bt_removeTag);
                TextView textView = (TextView)parent.findViewById(R.id.tvTitle);

                Log.i(TagsAdapter.class.getName(), "btAdd tag " + textView.getText().toString());

                DBHelper helper = new DBHelper(context);
                Tag tag = helper.addTag(artigo, textView.getText().toString());
                rawList.add(new TagsListItem(tag.getPk(), tag.getTag()));
                notifyDataSetChanged();

                parentSearch.clearSearch();

                btAdd.setVisibility(View.INVISIBLE);
                btRemove.setVisibility(View.VISIBLE);
            }
        });

        viewHolder.btRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewGroup parent = (ViewGroup) v.getParent();
                TextView textView = (TextView)parent.findViewById(R.id.tvTitle);
                ImageButton btAdd = (ImageButton)parent.findViewById(R.id.bt_addTag);
                ImageButton btRemove = (ImageButton)parent.findViewById(R.id.bt_removeTag);

                Log.i(TagsAdapter.class.getName(), "btRemove tag " + textView.getText().toString());

                DBHelper helper = new DBHelper(context);
                helper.removeTag(artigo, textView.getText().toString());
                notifyDataSetChanged();

                btAdd.setVisibility(View.VISIBLE);
                btRemove.setVisibility(View.INVISIBLE);
            }
        });

        return convertView;
    }

    @Override
    public Filter getFilter() {
        if (filter == null)
            filter = new TagsFilter();
        return filter;
    }

    @Override
    public int getCount() {
        return filteredList.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public TagsListItem getItem(int position) {
        return filteredList.get(position);
    }

    private Context getContext() {
        return context;
    }


    private static class ViewHolder {
        TextView tvTitle;
        ImageButton btAdd;
        ImageButton btRemove;
    }

    private class TagsFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            Log.i(TagsFilter.class.getName(), "Search " + constraint);

            FilterResults results = new FilterResults();
            if (constraint != null && constraint.length() > 0) {
                String filterText = constraint.toString().toLowerCase();
                ArrayList temp = new ArrayList();

                for (TagsListItem item: rawList) {
                    if (item.getTag().toLowerCase().contains(filterText))
                        temp.add(item);
                }

                for (String t: helper.queryTags(artigo, constraint.toString())) {
                    temp.add(new TagsListItem(0, t, true));
                }

                results.count = temp.size();
                results.values = temp;
            }
            else {
                results.count = rawList.size();
                results.values = rawList;
            }
            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredList = (ArrayList)results.values;
            notifyDataSetChanged();
        }
    }

}