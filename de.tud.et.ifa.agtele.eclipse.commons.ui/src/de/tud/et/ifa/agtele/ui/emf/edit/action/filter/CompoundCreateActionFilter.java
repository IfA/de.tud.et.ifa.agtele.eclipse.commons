package de.tud.et.ifa.agtele.ui.emf.edit.action.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.emf.edit.command.CommandParameter;
import org.eclipse.jface.dialogs.IDialogSettings;

public class CompoundCreateActionFilter extends CreateActionFilter implements ICompoundCreateActionFilter {

	protected List<ICreateActionFilter> subFilters = new ArrayList<>();
	
	protected CompoundFilterType type = CompoundFilterType.SIMPLE_GROUP;
	
	public CompoundCreateActionFilter() {}
	
	public CompoundCreateActionFilter(CompoundFilterType type) {
		this.type = type;
	}	
	
	@Override
	public boolean isActive() {
		return this.subFilters.stream().anyMatch(f -> f.isActive());
	}
	
	@Override
	public void persist(IDialogSettings settings) {
		this.doPersist(settings);
		this.subFilters.forEach(f -> {
			if (f.getContext() != null && !f.getContext().isBlank()) {
				IDialogSettings subSettings = settings.addNewSection(f.getContext());
				f.persist(subSettings);
			}
		});
	}

	@Override
	public void restore(IDialogSettings settings) {
		this.doRestore(settings);
		this.subFilters.forEach(f -> {
			if (f.getContext() != null && !f.getContext().isBlank()) {
				IDialogSettings subSettings = settings.getSection(f.getContext());
				if (subSettings != null) {
					f.restore(subSettings);					
				}
			}
		});
	}

	@Override
	public List<ICreateActionFilter> getSubFilters() {
		return new ArrayList<>(this.subFilters);
	}
	
	protected boolean isRadioGroup () {
		return this.type == CompoundFilterType.RADIO_GROUP || this.type == CompoundFilterType.RADIO_GROUP_ALWAYS_ON;
	}

	@Override
	public void addSubFilter(ICreateActionFilter filter, FilterChangedNotification notification) {	
		FilterChangedNotification myNotification = notification != null ? notification : new FilterChangedNotification();
		if (!this.subFilters.contains(filter)) {
			if (filter.isActive()) {
				if (this.isActive()) {
					this.subFilters.add(filter);
					filter.setOwner(this);
					if (this.isRadioGroup()) {
						this.subFilters.forEach(f -> {
							if (f != filter) {
								f.deactivate(myNotification);
							}
						});
					}
				} else {
					this.subFilters.add(filter);
					filter.setOwner(this);
				}
				myNotification.addChangedFilter(filter);
			} else {
				this.subFilters.add(filter);		
				filter.setOwner(this);		
			}
			filter.addListener(this);
		}
		if (this.type == CompoundFilterType.RADIO_GROUP_ALWAYS_ON && !this.isActive()) {
			this.activate(myNotification);
		}
		if (notification == null) {
			this.dispatchNotification(myNotification);			
		}
	}

	@Override
	public void removeSubFilter(ICreateActionFilter filter, FilterChangedNotification notification) {
		if (this.subFilters.contains(filter)) {		
			FilterChangedNotification myNotification = notification != null ? notification : new FilterChangedNotification();
			if (!filter.isActive()) {
				this.subFilters.remove(filter);
				filter.setOwner(null);
			} else {
				myNotification.addChangedFilter(filter);
				if (this.type == CompoundFilterType.RADIO_GROUP_ALWAYS_ON) {
					this.subFilters.remove(filter);
					filter.setOwner(null);
					this.activate(myNotification);
				}
			}
			filter.removeListener(this);
			if (notification == null) {
				this.dispatchNotification(myNotification);			
			}	
		}
	}
	
	@Override
	public void setActive(boolean active, FilterChangedNotification notification) {
		FilterChangedNotification myNotification = notification != null ? notification : new FilterChangedNotification();
		if (this.isActive() && !active) {
			if (this.type == CompoundFilterType.RADIO_GROUP_ALWAYS_ON) {
				return;
			}
			this.subFilters.forEach(f -> f.setActive(active, myNotification));
		} else if (!this.isActive() && active) {
			for (ICreateActionFilter filter : this.subFilters) {
				filter.activate(myNotification);
				if (filter.isActive()) {
					break;
				}
			}
		}
		if (notification == null) {
			this.dispatchNotification(myNotification);
		}
	}

	@Override
	public boolean isChangeAllowed(ICreateActionFilter filter) {
		if (!filter.isActive() || !this.isRadioGroup()) {
			return true;
		}
		return this.isChangeAllowed();
	}
	
	protected List<ICreateActionFilter> getActiveFilters () {
		return this.subFilters.stream().filter(f -> f.isActive()).collect(Collectors.toList());
	}

	@Override
	public List<CommandParameter> filterCommands(List<? extends CommandParameter> currentFilterState, List<? extends CommandParameter> originalCommands) {
		List<CommandParameter> result = new ArrayList<>(currentFilterState);
		
		for (ICreateActionFilter filter : this.getActiveFilters()) {
			result = filter.filterCommands(result, originalCommands);
		}		
		return result;
	}

	@Override
	public void notifySubFilterChange(ICreateActionFilter filter, FilterChangedNotification notification) {
		if (filter.isActive() && this.isActive() && this.isRadioGroup()) {
			for (ICreateActionFilter subFilter : this.subFilters) {
				if (subFilter != filter) {
					subFilter.deactivate(notification);
				}
			}
		}		
	}

	@Override
	public void notifiy(FilterChangedNotification notification) {
		if (this.getOwner() == null) {
			this.notifiyListeners(notification);
		} else {
			this.getOwner().notifiy(notification);
		}
	}

}
