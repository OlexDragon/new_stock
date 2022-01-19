
function onClick(id){

	if(id){

		$post = $.post('/components/single', {key : id})
		.done(function(componentData){

			if(!componentData.component){
				var $row = $('#' + id);
				var pn = $row.children('.partNumber').text();
				var mfrPN = $row.children('.mfrPN').text();
				var description = $row.children('.description').text();
				componentData.component = {Ref_Key: id, SKU: pn, MfrPNs: mfrPN, Description: description, ProductsType: ''}
			}

// Part Number
			$('#modal_title').text(componentData.component.SKU);

			var $body = $('#modal-body').empty();

// Description
			$('<div>', {class: 'row'}).appendTo($body)
			.append($('<div>', {class: 'col-4 text-right text-secondary'}).text('Description: '))
			.append($('<div>', {class: 'col text-left bg-light'}).append($('<strong>').text(componentData.component.Description)));

// Mfr Part Number
			$('<div>', {class: 'row'}).appendTo($body)
			.append($('<div>', {class: 'col-4 text-right text-secondary'}).text('Mfr PN: '))
			.append($('<div>', {class: 'col text-left bg-light'}).append($('<strong>').text(componentData.component.MfrPNs)));

// Product Type
			$('<div>', {class: 'row'}).appendTo($body)
			.append($('<div>', {class: 'col-4 text-right text-secondary'}).text('Product Type: '))
			.append($('<div>', {class: 'col text-left bg-light'}).text(componentData.component.ProductsType));

			if(componentData.componentQuantityResponse){

				var qty = componentData.componentQuantityResponse.value;
				$.each(qty, function(index, cq){
					$('<div>', {class: 'row ' + cq.StructuralUnit_Key}).appendTo($body)
// Location
					.append($('<div>', {class: 'col-4 text-right text-secondary'})	.text('Location: '))
					.append($('<div>', {class: 'col text-leftbg-light'}).append($('<strong>').text(cq.StructuralUnit.Description)))

//Next line
					.append($('<div>', {class: 'w-100'}))

// Quantity
					.append($('<div>', {class: 'col-4 text-right text-secondary'})	.text('Quantity: '))
					.append($('<div>', {class: 'col text-left bg-light'}).append($('<strong>').text(cq.QuantityBalance)));

				});
			}

// Links to files
			if(componentData.relatedFilesResponse && componentData.relatedFilesResponse.value.length>0){

				$('<div>', {class: 'row text-success ml-1'}).appendTo($body).text('Related Files:');

				$.each(componentData.relatedFilesResponse.value, function(index, link){

					$('<div>', {class: 'row ml-4 hover-almond'}).appendTo($body)
					.append($('<a>', {target: '_blank', href: '/files?path=' +  encodeURIComponent(link.PathToFile), class: 'text-success'}).text(link.PathToFile));
				});
			}

// BOMs
			if(componentData.bomsResponse && componentData.bomsResponse.value && componentData.bomsResponse.value.length>0){

				$('<div>', {class: 'row text-primary ml-1'}).appendTo($body).text('Used in...');

				$.each(componentData.bomsResponse.value, function(index, bom){
					if(bom.Status=='Active'){

						$('<div>', {class: 'row ml-1 hover-light'}).appendTo($body)
						.append($('<div>', {class: 'col-5'}).append($('<a>', {target: '_blank', href: "bom?key=" + bom.Ref_Key}).text(bom.Owner.SKU)))
						.append($('<div>', {class: 'col'}).append($('<a>', {target: '_blank', href: "bom?key=" + bom.Ref_Key}).text(bom.Description)));
					}
				});
			}

			$('#modal').modal('show');
		})
		.fail(function(error) {
			if(error.statusText!='abort')
				alert(error.responseText);
		});
	}
};
