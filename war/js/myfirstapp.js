// Let Sencha (Ext Core, actually) know about the "namespaces" we intend to use.
Ext.namespace('MyApp');

Ext.setup({
    tabletStartupScreen: 'images/tablet_startup.png',
    phoneStartupScreen: 'images/phone_startup.png',
    icon: 'images/icon.png',
    glossOnIcon: true,
    
    onReady: function() {
        var app = new MyApp.App();
    }
});

// I see this everywhere, but I do not have an Android device to look into the reasoning for it.
// I'm guessing that the animations do not work on Android or that they are not generally used.
MyApp.defaultAnim = Ext.is.Android ? false : 'slide';

MyApp.MyForm = Ext.extend(Ext.form.FormPanel, {
	title: 'Form',
    items: [
            {
                xtype: 'textfield',
                name : 'first',
                label: 'First name'
            },
            {
                xtype: 'textfield',
                name : 'last',
                label: 'Last name'
            },
            {
                xtype: 'numberfield',
                name : 'age',
                label: 'Age'
            },
            {
                xtype: 'urlfield',
                name : 'url',
                label: 'Website'
            }
        ]
});

MyApp.bookstore = new Ext.data.JsonStore({
    autoDestroy: true,
    autoLoad: true,
    storeId: 'bookStore',
    proxy: {
        type: 'rest',
        url: '/resources/books',
        reader: {
            type: 'json',
            root: 'books'
        }
    },
    
    fields: ['title', 'description', 'imageUrl', 'feedUrl', 'url', 'lastUpdated', 'authors', 'categories', 'copyright']
});

MyApp.BookList = Ext.extend(Ext.List, {
	title: 'Books',
    itemSelector: 'li.book-item',
    singleSelect: true,
    
    initComponent: function() {
    	this.tpl = Ext.XTemplate.from('recent');
        this.store = MyApp.bookstore;
        this.on('itemtap', this.onItemTap, this);
        MyApp.BookList.superclass.initComponent.call(this);
    },

	onItemTap: function(item, index) {
		var book = MyApp.bookstore.getAt(index);
		this.fireEvent('bookSelect', book.get('title'));
	}
});

MyApp.BookDetail = Ext.extend(Ext.Container, {
	cls: 'detail',
	title: 'Detail',
    initComponent: function() {
    	this.tpl = Ext.XTemplate.from('detail');
        MyApp.BookDetail.superclass.initComponent.call(this);
    }
});

// MyApp extends TabPanel, but you can use Panel or Container or whatever else floats your boat.
MyApp.App = Ext.extend(Ext.TabPanel, {
	cls: 'myapp',
	fullscreen: true,
	layout: 'card',
	activeItem: 0,
	items: [{
        title: 'Yoda',
        html: 'Do or do not. There is no try.',
        cls: 'card1'
    }, {
    	xtype: 'map',
        title: 'Map',
        getLocation: true
    }, {
        title: 'Boom',
        html: 'Boom goes the dynamite.',
        cls: 'card3'
    }, {
    	xtype: 'form',
    	title: 'Form',
        items: [
                {
                    xtype: 'textfield',
                    name : 'first',
                    label: 'First name'
                },
                {
                    xtype: 'textfield',
                    name : 'last',
                    label: 'Last name'
                },
                {
                    xtype: 'numberfield',
                    name : 'age',
                    label: 'Age'
                },
                {
                    xtype: 'urlfield',
                    name : 'url',
                    label: 'Website'
                },
                {
                	xtype: 'slider',
                	name: 'slide',
                	label: 'Whee!'
                }
            ]
    }],
    detail: null,
    booklist: null,
	initComponent: function() {
		this.booklist = new MyApp.BookList(); 
		MyApp.App.superclass.initComponent.call(this);
		this.booklist.on('bookSelect', this.showBook, this);
		this.detail = new MyApp.BookDetail();
		this.add(this.booklist);
		this.add(this.detail);
	},
	showBook: function(title){
		var index = MyApp.bookstore.find('title', title);
        this.setCard(this.detail, MyApp.defaultAnim);
        this.detail.update(MyApp.bookstore.getAt(index).data);
	}
});